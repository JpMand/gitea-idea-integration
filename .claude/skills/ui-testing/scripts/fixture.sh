#!/usr/bin/env bash
# Builds the acme/webapp test fixture on the local Gitea (container "gitea", http://localhost:3000).
# tea runs INSIDE the container; git runs on the host, committing/pushing as each Gitea user.
# Tokens (root/alice/bob/carol.token) and scratch files go to $GITEA_UI_STATE (default ~/.gitea-ui).
set -euo pipefail
S=${GITEA_UI_STATE:-$HOME/.gitea-ui}; mkdir -p "$S"
W=$S/fixture
PW='Passw0rd!'
URL=http://localhost:3000
T() { docker exec -i gitea tea "$@"; }
say() { printf '\n### %s\n' "$*"; }

say "tea login as root (creates a token for tea)"
T logins add --name root --url $URL --user root --password "$PW" --scopes all --no-version-check >/dev/null
T logins default root >/dev/null

say "users"
for u in alice bob carol; do
  T admin users create --username $u --password "$PW" --email $u@example.com --full-name "${u^} Example" --no-must-change-password
done

say "per-user tea logins (each creates an access token)"
for u in alice bob carol; do
  T logins add --name $u --url $URL --user $u --password "$PW" --scopes all --no-version-check >/dev/null
done
docker exec gitea sh -c 'cat /root/.config/tea/config.yml' > $S/tea-config.yml
tok() { python3 - "$1" <<'EOF'
import sys, re
name = sys.argv[1]
txt = open(__import__('os').environ['S'] + '/tea-config.yml').read()
for block in re.split(r'\n\s*- name: ', '\n' + txt)[1:]:
    if block.split('\n')[0].strip() == name:
        print(re.search(r'token: (\S+)', block).group(1)); break
EOF
}
export S
for u in root alice bob carol; do tok $u > $S/$u.token; done
wc -c $S/*.token

say "org + repo + collaborators"
T org create acme --full-name "ACME Corp" --description "Test organisation"
T repos create --owner acme --name webapp --description "Demo app for the Gitea IDE plugin" --output simple
for u in alice bob carol; do
  T api -X PUT "/repos/acme/webapp/collaborators/$u" -d '{"permission":"write"}' >/dev/null
done
T api /repos/acme/webapp/collaborators | python3 -c 'import json,sys; print("collaborators:", [c["login"] for c in json.load(sys.stdin)])'

say "labels + milestone"
T labels create --repo acme/webapp --name bug --color '#ee0701' --description "Something is broken"
T labels create --repo acme/webapp --name enhancement --color '#84b6eb' --description "New feature"
T labels create --repo acme/webapp --name documentation --color '#0075ca' --description "Docs only"
T milestones create --repo acme/webapp --title v1.0 --description "First release"

say "git: initial main (carol)"
rm -rf $W && mkdir -p $W && cd $W
git init -q -b main webapp && cd webapp
remote() { echo "http://$1:$(cat $S/$1.token)@localhost:3000/acme/webapp.git"; }
as() { export GIT_AUTHOR_NAME="${1^} Example" GIT_AUTHOR_EMAIL=$1@example.com GIT_COMMITTER_NAME="${1^} Example" GIT_COMMITTER_EMAIL=$1@example.com; ME=$1; }
push() { git push -q "$(remote $ME)" "$@"; }
as carol
mkdir -p src/main/java/com/acme docs
cat > README.md <<'EOF'
# webapp

Demo application used to exercise the Gitea IDE plugin.
EOF
cat > docs/notes.md <<'EOF'
# Notes

Scratch notes; to be removed.
EOF
cat > src/main/java/com/acme/App.java <<'EOF'
package com.acme;

public class App {
    public static void main(String[] args) {
        Config config = Config.load();
        System.out.println("Starting " + config.name());
    }
}
EOF
cat > src/main/java/com/acme/Config.java <<'EOF'
package com.acme;

public record Config(String name, int port) {

    public static Config load() {
        String name = System.getProperty("app.name", "webapp");
        int port = Integer.getInteger("app.port", 8080);
        return new Config(name, port);
    }
}
EOF
cat > src/main/java/com/acme/Util.java <<'EOF'
package com.acme;

public final class Util {
    private Util() {}

    public static String trim(String s) {
        return s.trim();
    }
}
EOF
git add -A && git commit -qm "Initial project skeleton" && push main

say "branch feature/greeting (bob, 3 commits)"
as bob
git checkout -qb feature/greeting main
cat > src/main/java/com/acme/GreetingService.java <<'EOF'
package com.acme;

import java.util.Objects;

/**
 * Builds greetings for users.
 */
public class GreetingService {

    private final String defaultName = "World";

    public String greet(String name) {
        String target = name == null ? defaultName : name;
        Objects.requireNonNull(target);
        return "Hello, " + target + "!";
    }

    public String farewell(String name) {
        return "Goodbye, " + name + ".";
    }
}
EOF
git add -A && git commit -qm "Add GreetingService"
cat > src/main/java/com/acme/App.java <<'EOF'
package com.acme;

public class App {
    public static void main(String[] args) {
        Config config = Config.load();
        System.out.println("Starting " + config.name());
        GreetingService greetings = new GreetingService();
        System.out.println(greetings.greet(args.length > 0 ? args[0] : null));
    }
}
EOF
git add -A && git commit -qm "Use GreetingService from App" -m "App now prints a greeting for the first CLI argument, falling back to the
default name when no argument is given." -m "This keeps main() tiny: all greeting logic lives in GreetingService so it
can be unit tested without spawning the app.

- greet() handles null
- farewell() is not wired up yet

Refs: #1"
mkdir -p src/test/java/com/acme
cat > src/test/java/com/acme/GreetingServiceTest.java <<'EOF'
package com.acme;

public class GreetingServiceTest {
    public static void main(String[] args) {
        GreetingService s = new GreetingService();
        if (!s.greet(null).equals("Hello, World!")) throw new AssertionError();
        if (!s.greet("Ann").equals("Hello, Ann!")) throw new AssertionError();
    }
}
EOF
git rm -q docs/notes.md
git add -A && git commit -qm "Add GreetingService test, drop scratch notes"
push feature/greeting

say "branch refactor/config (alice) - will conflict with main"
as alice
git checkout -qb refactor/config main
sed -i 's|System.getProperty("app.name", "webapp")|System.getenv().getOrDefault("APP_NAME", "webapp")|; s|Integer.getInteger("app.port", 8080)|Integer.parseInt(System.getenv().getOrDefault("APP_PORT", "8080"))|' src/main/java/com/acme/Config.java
git commit -qam "Read config from environment variables" && push refactor/config

say "branch docs/readme (carol)"
as carol
git checkout -qb docs/readme main
printf '\n## Running\n\n```\njava src/main/java/com/acme/App.java\n```\n' >> README.md
git commit -qam "Document how to run the app" && push docs/readme

say "branch wip/cache (bob)"
as bob
git checkout -qb wip/cache main
cat > src/main/java/com/acme/Cache.java <<'EOF'
package com.acme;

import java.util.HashMap;
import java.util.Map;

public class Cache<K, V> {
    private final Map<K, V> map = new HashMap<>();

    public V get(K key) { return map.get(key); }

    public void put(K key, V value) { map.put(key, value); }
}
EOF
git add -A && git commit -qm "Experimental in-memory cache" && push wip/cache

say "branch old/logging (carol)"
as carol
git checkout -qb old/logging main
sed -i 's|System.out.println("Starting " + config.name());|System.err.println("[LOG] Starting " + config.name());|' src/main/java/com/acme/App.java
git commit -qam "Log to stderr" && push old/logging

say "branch ci/setup (alice)"
as alice
git checkout -qb ci/setup main
mkdir -p ci && printf '#!/bin/sh\nset -e\njavac -d out $(find src/main -name "*.java")\n' > ci/build.sh && chmod +x ci/build.sh
git add -A && git commit -qm "Add CI build script" && push ci/setup

say "branch fix/null (bob)"
as bob
git checkout -qb fix/null main
sed -i 's|return s.trim();|return s == null ? "" : s.trim();|' src/main/java/com/acme/Util.java
git commit -qam "Handle null in Util.trim" && push fix/null
git checkout -q main

say "pull requests"
cat > $S/pr1.md <<'EOF'
Adds a `GreetingService` and wires it into `App`.

## Changes
- New `GreetingService` with `greet` / `farewell`
- `App` prints a greeting for the first CLI argument
- Test for the default greeting
- Removes the old scratch notes

## Testing
Ran `GreetingServiceTest` locally.

<details><summary>Background</summary>

This is the first step towards localised greetings. A follow-up will read the default name from `Config`,
and after that we will add a resource bundle per locale so that greetings can be translated. The farewell
message is intentionally simple for now and will get the same treatment.

</details>
EOF
T pr create --login bob --repo acme/webapp --head feature/greeting --base main --title "Add greeting service" --description-file - --labels enhancement --milestone v1.0 --assignees alice < $S/pr1.md
T pr create --login alice --repo acme/webapp --head refactor/config --base main --title "Refactor config loader to use env vars" --description "Reads APP_NAME / APP_PORT from the environment instead of system properties."
T pr create --login carol --repo acme/webapp --head docs/readme --base main --title "Document how to run the app" --description "README: add a Running section." --labels documentation
T pr create --login bob --repo acme/webapp --head wip/cache --base main --draft --title "Experimental cache" --description "Not ready - just exploring." --labels bug
T pr create --login carol --repo acme/webapp --head old/logging --base main --title "Log startup to stderr" --description "Superseded, will close."
T pr create --login alice --repo acme/webapp --head ci/setup --base main --title "Add CI build script" --description "Simple javac build for CI."
T pr create --login bob --repo acme/webapp --head fix/null --base main --title "Handle null in Util.trim" --description "Avoid NPE when trimming null." --labels bug

say "close #5, merge #6"
T pr close --login carol --repo acme/webapp 5
T pr merge --login root --repo acme/webapp --style merge 6

say "main moves: conflicting change for #2 (root)"
as root
git pull -q "$(remote root)" main
sed -i 's|System.getProperty("app.name", "webapp")|System.getProperty("app.name", "acme-webapp")|; s|Integer.getInteger("app.port", 8080)|Integer.getInteger("app.port", 9090)|' src/main/java/com/acme/Config.java
git commit -qam "Change default app name and port" && push main

say "review requests"
T pr edit --login bob --repo acme/webapp --add-reviewers alice,carol 1
T pr edit --login bob --repo acme/webapp --add-reviewers alice 7
T pr edit --login carol --repo acme/webapp --add-reviewers alice 3

say "PR #1: carol's review with inline threads (incl. suggestion)"
HEAD1=$(git rev-parse feature/greeting)
python3 - "$HEAD1" > $S/review1.json <<'PYEOF'
import json, sys
sug = ("Prefer `String.format` here so the template can later move into a resource bundle.\n\n"
       "<!--gitea-suggestion-->\n```diff\n@@ -15,1 +15,1 @@\n"
       "-        return \"Hello, \" + target + \"!\";\n"
       "+        return String.format(\"Hello, %s!\", target);\n```")
print(json.dumps({
  "commit_id": sys.argv[1], "event": "REQUEST_CHANGES",
  "body": "Nice start! A few things to address before this goes in.",
  "comments": [
    {"path": "src/main/java/com/acme/GreetingService.java", "new_position": 10, "body": "Should the default name be configurable?"},
    {"path": "src/main/java/com/acme/GreetingService.java", "new_position": 15, "body": sug},
    {"path": "src/main/java/com/acme/GreetingService.java", "new_position": 19, "body": "`farewell` doesn't handle `null` the way `greet` does."},
    {"path": "src/main/java/com/acme/App.java", "new_position": 7, "body": "Could this be a field instead of a local?"},
  ]}))
PYEOF
T api -X POST /repos/acme/webapp/pulls/1/reviews --login carol -d @- < $S/review1.json > $S/review1.out.json
python3 -c "import json;r=json.load(open('$S/review1.out.json'));print('review',r['id'],r['state'],r['comments_count'])"
T api "/repos/acme/webapp/pulls/1/reviews" --login bob > $S/reviews1.json
RID=$(python3 -c "import json;print([r['id'] for r in json.load(open('$S/reviews1.json')) if r['user']['login']=='carol'][0])")
T api "/repos/acme/webapp/pulls/1/reviews/$RID/comments" --login bob > $S/rc1.json
python3 -c "
import json
for c in json.load(open('$S/rc1.json')): print(c['id'], c['path'], c['position'], c['body'][:50].replace(chr(10),' '))" | tee $S/rc1.txt
C_DEFAULT=$(awk '$3==10 && /GreetingService/{print $1}' $S/rc1.txt)
C_APP=$(awk '/App.java/{print $1}' $S/rc1.txt)

say "bob replies on the 'configurable' thread (replies API), resolves the App.java thread"
T api -X POST "/repos/acme/webapp/pulls/1/comments/$C_DEFAULT/replies" --login bob -d "{\"body\":\"Good point - I'll read it from Config in a follow-up.\"}" >/dev/null
T pr resolve --login bob --repo acme/webapp "$C_APP"

say "general comments + commit statuses"
T comment --login carol --repo acme/webapp 1 "Thanks for picking this up! Left a few notes inline."
T comment --login bob --repo acme/webapp 1 "Pushed the test as well - PTAL."
T api -X POST "/repos/acme/webapp/statuses/$HEAD1" --login root -d '{"state":"success","context":"ci/build","description":"Build passed","target_url":"http://localhost:3000/acme/webapp"}' >/dev/null
T api -X POST "/repos/acme/webapp/statuses/$HEAD1" --login root -d '{"state":"pending","context":"ci/lint","description":"Lint running"}' >/dev/null

say "PR #7: approved by carol"
T pr approve --login carol --repo acme/webapp 7 "LGTM"

say "summary"
T pr list --login root --repo acme/webapp --state all --fields index,state,author,title,labels,mergeable,head
