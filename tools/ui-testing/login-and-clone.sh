#!/usr/bin/env bash
# First run on a fresh IDE: adds alice's Gitea account (Settings > Version Control > Gitea) from the
# welcome screen, then clones acme/webapp through Get from VCS > Gitea into ~/IdeaProjects/webapp.
set -e
D=$(cd "$(dirname "$0")" && pwd); U=$D/ui.py; export DISPLAY=:99
S=${GITEA_UI_STATE:-$HOME/.gitea-ui}
$U click "//div[@class='FlatWelcomeFrame']" 0 400 20 >/dev/null
$U key ctrl+alt+s; $U wait "//div[@class='MyDialog' and @accessiblename='Settings']" 20 >/dev/null; sleep 2
$U type Gitea; sleep 2
$U wait "//div[@accessiblename='Add' and @class='ActionButton']" 20 >/dev/null; $U click "//div[@accessiblename='Add' and @class='ActionButton']" >/dev/null
$U wait "//div[@class='MyDialog' and @accessiblename='Add Gitea Account']" 10 >/dev/null
$U click "//div[@class='ExtendableTextField' and @accessiblename='Server:']" >/dev/null; $U key ctrl+a; $U type "http://localhost:3000"
$U click "//div[@class='JBPasswordField']" >/dev/null; $U type "$(cat $S/alice.token)"
$U click "//div[@class='JButton' and @text='Log In']" >/dev/null; sleep 4
$U tree "^MyDialog acce='Settings'" | grep -o "JBList visi='[^']*'"
$U click "//div[@class='JButton' and @text='OK']" >/dev/null; sleep 2
$D/clear-notifications.sh; sleep 1
$U click "//div[@class='JButton' and @accessiblename='Clone Repository']" >/dev/null; sleep 3
$U clicktext "//div[@class='VcsCloneDialogExtensionList']" "Gitea" >/dev/null; sleep 3
$U clicktext "//div[@class='JBList' and contains(@visible_text,'acme/webapp')]" "acme/webapp" >/dev/null; sleep 1
$U click "//div[@class='JButton' and @text='Clone']" >/dev/null
for i in $(seq 1 60); do xdotool search --onlyvisible --name 'webapp' >/dev/null 2>&1 && break; sleep 2; done; sleep 8
W=$(xdotool search --onlyvisible --name 'webapp' | head -1); xdotool windowmove $W 0 0; xdotool windowsize $W 1600 1000
git -C /root/IdeaProjects/webapp config user.name "Alice Example"; git -C /root/IdeaProjects/webapp config user.email alice@example.com
echo "cloned: $(git -C /root/IdeaProjects/webapp log --oneline -1)"
