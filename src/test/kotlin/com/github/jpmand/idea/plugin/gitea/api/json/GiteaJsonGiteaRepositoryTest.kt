package com.github.jpmand.idea.plugin.gitea.api.json

import com.github.jpmand.idea.plugin.gitea.api.GiteaJsonDeSerializer
import com.github.jpmand.idea.plugin.gitea.api.rest.models.GiteaRepositoryDTO
import com.intellij.util.containers.forEachGuaranteed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.StringReader

/**
 * Tests for JSON deserialization of Gitea Repository API responses.
 * Verifies that snake_case JSON field names are correctly mapped to their Kotlin counterparts
 * via the SNAKE_CASE naming strategy, consistent with the Gitea REST API format documented at
 * https://gitea.com/api/swagger (Repository definition).
 */
class GiteaJsonGiteaRepositoryTest {

  private fun <T> deserialize(json: String, clazz: Class<T>): T? =
    GiteaJsonDeSerializer.fromJson(StringReader(json), clazz)

  val data : String = """
    [
    {
      "id": 113277,
      "owner": {
        "id": 156110,
        "login": "evallab01",
        "login_name": "",
        "source_id": 0,
        "full_name": "",
        "email": "156110+evallab01@noreply.gitea.com",
        "avatar_url": "https://seccdn.libravatar.org/avatar/f6171ad419e3ecfe50ed75a9ba5135af?d=identicon",
        "html_url": "https://gitea.com/evallab01",
        "language": "",
        "is_admin": false,
        "last_login": "0001-01-01T00:00:00Z",
        "created": "2025-11-08T12:07:31Z",
        "restricted": false,
        "active": false,
        "prohibit_login": false,
        "location": "",
        "website": "",
        "description": "",
        "visibility": "public",
        "followers_count": 0,
        "following_count": 0,
        "starred_repos_count": 0,
        "username": "evallab01"
      },
      "name": "-a-href-javascript-alert--XSS---Click-here-for-XSS-test--a-",
      "full_name": "evallab01/-a-href-javascript-alert--XSS---Click-here-for-XSS-test--a-",
      "description": "",
      "empty": true,
      "private": false,
      "fork": false,
      "template": false,
      "mirror": false,
      "size": 27,
      "language": "",
      "languages_url": "https://gitea.com/api/v1/repos/evallab01/-a-href-javascript-alert--XSS---Click-here-for-XSS-test--a-/languages",
      "html_url": "https://gitea.com/evallab01/-a-href-javascript-alert--XSS---Click-here-for-XSS-test--a-",
      "url": "https://gitea.com/api/v1/repos/evallab01/-a-href-javascript-alert--XSS---Click-here-for-XSS-test--a-",
      "link": "",
      "ssh_url": "git@gitea.com:evallab01/-a-href-javascript-alert--XSS---Click-here-for-XSS-test--a-.git",
      "clone_url": "https://gitea.com/evallab01/-a-href-javascript-alert--XSS---Click-here-for-XSS-test--a-.git",
      "original_url": "",
      "website": "",
      "stars_count": 0,
      "forks_count": 0,
      "watchers_count": 1,
      "open_issues_count": 0,
      "open_pr_counter": 0,
      "release_counter": 0,
      "default_branch": "main",
      "archived": false,
      "created_at": "2025-11-08T12:25:30Z",
      "updated_at": "2025-11-08T12:25:30Z",
      "archived_at": "1970-01-01T00:00:00Z",
      "permissions": {
        "admin": false,
        "push": false,
        "pull": true
      },
      "has_code": true,
      "has_issues": true,
      "internal_tracker": {
        "enable_time_tracker": true,
        "allow_only_contributors_to_track_time": true,
        "enable_issue_dependencies": true
      },
      "has_wiki": true,
      "has_pull_requests": true,
      "has_projects": true,
      "projects_mode": "all",
      "has_releases": true,
      "has_packages": false,
      "has_actions": true,
      "ignore_whitespace_conflicts": false,
      "allow_merge_commits": true,
      "allow_rebase": true,
      "allow_rebase_explicit": true,
      "allow_squash_merge": true,
      "allow_fast_forward_only_merge": true,
      "allow_rebase_update": true,
      "allow_manual_merge": false,
      "autodetect_manual_merge": false,
      "default_delete_branch_after_merge": false,
      "default_merge_style": "merge",
      "default_allow_maintainer_edit": false,
      "avatar_url": "",
      "internal": false,
      "mirror_interval": "",
      "object_format_name": "sha1",
      "mirror_updated": "0001-01-01T00:00:00Z",
      "topics": [],
      "licenses": []
    },
    {
      "id": 86444,
      "owner": {
        "id": 100043,
        "login": "egorlavrinovich",
        "login_name": "",
        "source_id": 0,
        "full_name": "",
        "email": "100043+egorlavrinovich@noreply.gitea.com",
        "avatar_url": "https://seccdn.libravatar.org/avatar/23901cc528a5e88af664cee01bf18e33?d=identicon",
        "html_url": "https://gitea.com/egorlavrinovich",
        "language": "",
        "is_admin": false,
        "last_login": "0001-01-01T00:00:00Z",
        "created": "2025-01-15T11:57:53Z",
        "restricted": false,
        "active": false,
        "prohibit_login": false,
        "location": "",
        "website": "",
        "description": "",
        "visibility": "public",
        "followers_count": 0,
        "following_count": 0,
        "starred_repos_count": 0,
        "username": "egorlavrinovich"
      },
      "name": ".test",
      "full_name": "egorlavrinovich/.test",
      "description": "",
      "empty": true,
      "private": false,
      "fork": false,
      "template": false,
      "mirror": false,
      "size": 27,
      "language": "",
      "languages_url": "https://gitea.com/api/v1/repos/egorlavrinovich/.test/languages",
      "html_url": "https://gitea.com/egorlavrinovich/.test",
      "url": "https://gitea.com/api/v1/repos/egorlavrinovich/.test",
      "link": "",
      "ssh_url": "git@gitea.com:egorlavrinovich/.test.git",
      "clone_url": "https://gitea.com/egorlavrinovich/.test.git",
      "original_url": "",
      "website": "",
      "stars_count": 0,
      "forks_count": 0,
      "watchers_count": 1,
      "open_issues_count": 2,
      "open_pr_counter": 0,
      "release_counter": 0,
      "default_branch": "",
      "archived": false,
      "created_at": "2025-01-15T12:00:33Z",
      "updated_at": "2025-02-03T13:07:14Z",
      "archived_at": "1970-01-01T00:00:00Z",
      "permissions": {
        "admin": false,
        "push": false,
        "pull": true
      },
      "has_code": true,
      "has_issues": true,
      "internal_tracker": {
        "enable_time_tracker": true,
        "allow_only_contributors_to_track_time": true,
        "enable_issue_dependencies": true
      },
      "has_wiki": true,
      "has_pull_requests": true,
      "has_projects": true,
      "projects_mode": "all",
      "has_releases": true,
      "has_packages": false,
      "has_actions": true,
      "ignore_whitespace_conflicts": false,
      "allow_merge_commits": true,
      "allow_rebase": true,
      "allow_rebase_explicit": true,
      "allow_squash_merge": true,
      "allow_fast_forward_only_merge": true,
      "allow_rebase_update": true,
      "allow_manual_merge": false,
      "autodetect_manual_merge": false,
      "default_delete_branch_after_merge": false,
      "default_merge_style": "merge",
      "default_allow_maintainer_edit": false,
      "avatar_url": "",
      "internal": false,
      "mirror_interval": "",
      "object_format_name": "sha1",
      "mirror_updated": "0001-01-01T00:00:00Z",
      "topics": [],
      "licenses": []
    },
    {
      "id": 86592,
      "owner": {
        "id": 100129,
        "login": "orgtest12",
        "login_name": "",
        "source_id": 0,
        "full_name": "",
        "email": "",
        "avatar_url": "https://gitea.com/avatars/80bfc925851ca4f49c2a6c365122ab36",
        "html_url": "https://gitea.com/orgtest12",
        "language": "",
        "is_admin": false,
        "last_login": "0001-01-01T00:00:00Z",
        "created": "2025-01-16T07:25:26Z",
        "restricted": false,
        "active": false,
        "prohibit_login": false,
        "location": "",
        "website": "",
        "description": "",
        "visibility": "public",
        "followers_count": 0,
        "following_count": 0,
        "starred_repos_count": 0,
        "username": "orgtest12"
      },
      "name": ".test",
      "full_name": "orgtest12/.test",
      "description": "",
      "empty": true,
      "private": false,
      "fork": false,
      "template": false,
      "mirror": false,
      "size": 27,
      "language": "",
      "languages_url": "https://gitea.com/api/v1/repos/orgtest12/.test/languages",
      "html_url": "https://gitea.com/orgtest12/.test",
      "url": "https://gitea.com/api/v1/repos/orgtest12/.test",
      "link": "",
      "ssh_url": "git@gitea.com:orgtest12/.test.git",
      "clone_url": "https://gitea.com/orgtest12/.test.git",
      "original_url": "",
      "website": "",
      "stars_count": 0,
      "forks_count": 0,
      "watchers_count": 1,
      "open_issues_count": 2,
      "open_pr_counter": 0,
      "release_counter": 0,
      "default_branch": "main",
      "archived": false,
      "created_at": "2025-01-16T07:26:09Z",
      "updated_at": "2025-01-16T07:26:09Z",
      "archived_at": "1970-01-01T00:00:00Z",
      "permissions": {
        "admin": false,
        "push": false,
        "pull": true
      },
      "has_code": true,
      "has_issues": true,
      "internal_tracker": {
        "enable_time_tracker": true,
        "allow_only_contributors_to_track_time": true,
        "enable_issue_dependencies": true
      },
      "has_wiki": true,
      "has_pull_requests": true,
      "has_projects": true,
      "projects_mode": "all",
      "has_releases": true,
      "has_packages": false,
      "has_actions": true,
      "ignore_whitespace_conflicts": false,
      "allow_merge_commits": true,
      "allow_rebase": true,
      "allow_rebase_explicit": true,
      "allow_squash_merge": true,
      "allow_fast_forward_only_merge": true,
      "allow_rebase_update": true,
      "allow_manual_merge": false,
      "autodetect_manual_merge": false,
      "default_delete_branch_after_merge": false,
      "default_merge_style": "merge",
      "default_allow_maintainer_edit": false,
      "avatar_url": "",
      "internal": false,
      "mirror_interval": "",
      "object_format_name": "sha1",
      "mirror_updated": "0001-01-01T00:00:00Z",
      "topics": [],
      "licenses": []
    },
    {
      "id": 107149,
      "owner": {
        "id": 141846,
        "login": "testxxx",
        "login_name": "",
        "source_id": 0,
        "full_name": "",
        "email": "",
        "avatar_url": "https://gitea.com/avatars/7cbab5cea99169139e7e6d8ff74ebb77",
        "html_url": "https://gitea.com/testxxx",
        "language": "",
        "is_admin": false,
        "last_login": "0001-01-01T00:00:00Z",
        "created": "2025-08-26T11:17:57Z",
        "restricted": false,
        "active": false,
        "prohibit_login": false,
        "location": "",
        "website": "",
        "description": "",
        "visibility": "public",
        "followers_count": 0,
        "following_count": 0,
        "starred_repos_count": 0,
        "username": "testxxx"
      },
      "name": "2test",
      "full_name": "testxxx/2test",
      "description": "",
      "empty": true,
      "private": false,
      "fork": false,
      "template": false,
      "mirror": false,
      "size": 27,
      "language": "",
      "languages_url": "https://gitea.com/api/v1/repos/testxxx/2test/languages",
      "html_url": "https://gitea.com/testxxx/2test",
      "url": "https://gitea.com/api/v1/repos/testxxx/2test",
      "link": "",
      "ssh_url": "git@gitea.com:testxxx/2test.git",
      "clone_url": "https://gitea.com/testxxx/2test.git",
      "original_url": "",
      "website": "",
      "stars_count": 0,
      "forks_count": 0,
      "watchers_count": 1,
      "open_issues_count": 0,
      "open_pr_counter": 0,
      "release_counter": 0,
      "default_branch": "main",
      "archived": false,
      "created_at": "2025-08-26T11:18:34Z",
      "updated_at": "2025-08-26T11:18:34Z",
      "archived_at": "1970-01-01T00:00:00Z",
      "permissions": {
        "admin": false,
        "push": false,
        "pull": true
      },
      "has_code": true,
      "has_issues": true,
      "internal_tracker": {
        "enable_time_tracker": true,
        "allow_only_contributors_to_track_time": true,
        "enable_issue_dependencies": true
      },
      "has_wiki": true,
      "has_pull_requests": true,
      "has_projects": true,
      "projects_mode": "all",
      "has_releases": true,
      "has_packages": false,
      "has_actions": true,
      "ignore_whitespace_conflicts": false,
      "allow_merge_commits": true,
      "allow_rebase": true,
      "allow_rebase_explicit": true,
      "allow_squash_merge": true,
      "allow_fast_forward_only_merge": true,
      "allow_rebase_update": true,
      "allow_manual_merge": false,
      "autodetect_manual_merge": false,
      "default_delete_branch_after_merge": false,
      "default_merge_style": "merge",
      "default_allow_maintainer_edit": false,
      "avatar_url": "",
      "internal": false,
      "mirror_interval": "",
      "object_format_name": "sha1",
      "mirror_updated": "0001-01-01T00:00:00Z",
      "topics": [],
      "licenses": []
    },
    {
      "id": 45897,
      "owner": {
        "id": 54240,
        "login": "AadheshAero",
        "login_name": "",
        "source_id": 0,
        "full_name": "",
        "email": "54240+aadheshaero@noreply.gitea.com",
        "avatar_url": "https://seccdn.libravatar.org/avatar/7262fbd5481cc5cc7bcaa31a3c752a84?d=identicon",
        "html_url": "https://gitea.com/AadheshAero",
        "language": "",
        "is_admin": false,
        "last_login": "0001-01-01T00:00:00Z",
        "created": "2023-11-22T18:34:29Z",
        "restricted": false,
        "active": false,
        "prohibit_login": false,
        "location": "",
        "website": "",
        "description": "",
        "visibility": "public",
        "followers_count": 0,
        "following_count": 0,
        "starred_repos_count": 0,
        "username": "AadheshAero"
      },
      "name": "Aadheshtest",
      "full_name": "AadheshAero/Aadheshtest",
      "description": "",
      "empty": false,
      "private": false,
      "fork": false,
      "template": false,
      "mirror": false,
      "size": 25,
      "language": "",
      "languages_url": "https://gitea.com/api/v1/repos/AadheshAero/Aadheshtest/languages",
      "html_url": "https://gitea.com/AadheshAero/Aadheshtest",
      "url": "https://gitea.com/api/v1/repos/AadheshAero/Aadheshtest",
      "link": "",
      "ssh_url": "git@gitea.com:AadheshAero/Aadheshtest.git",
      "clone_url": "https://gitea.com/AadheshAero/Aadheshtest.git",
      "original_url": "",
      "website": "",
      "stars_count": 0,
      "forks_count": 0,
      "watchers_count": 1,
      "open_issues_count": 0,
      "open_pr_counter": 0,
      "release_counter": 0,
      "default_branch": "master",
      "archived": false,
      "created_at": "2023-11-22T18:36:01Z",
      "updated_at": "2023-11-22T18:42:46Z",
      "archived_at": "1970-01-01T00:00:00Z",
      "permissions": {
        "admin": false,
        "push": false,
        "pull": true
      },
      "has_code": true,
      "has_issues": true,
      "internal_tracker": {
        "enable_time_tracker": true,
        "allow_only_contributors_to_track_time": true,
        "enable_issue_dependencies": true
      },
      "has_wiki": true,
      "has_pull_requests": true,
      "has_projects": true,
      "projects_mode": "",
      "has_releases": true,
      "has_packages": false,
      "has_actions": true,
      "ignore_whitespace_conflicts": false,
      "allow_merge_commits": true,
      "allow_rebase": true,
      "allow_rebase_explicit": true,
      "allow_squash_merge": true,
      "allow_fast_forward_only_merge": false,
      "allow_rebase_update": true,
      "allow_manual_merge": false,
      "autodetect_manual_merge": false,
      "default_delete_branch_after_merge": false,
      "default_merge_style": "merge",
      "default_allow_maintainer_edit": false,
      "avatar_url": "",
      "internal": false,
      "mirror_interval": "",
      "object_format_name": "sha1",
      "mirror_updated": "0001-01-01T00:00:00Z",
      "topics": [],
      "licenses": []
    },
    {
      "id": 123346,
      "owner": {
        "id": 178984,
        "login": "abla-test",
        "login_name": "",
        "source_id": 0,
        "full_name": "Alex Blais",
        "email": "178984+abla-test@noreply.gitea.com",
        "avatar_url": "https://seccdn.libravatar.org/avatar/df96c1dce88674ffd95266845576a560?d=identicon",
        "html_url": "https://gitea.com/abla-test",
        "language": "",
        "is_admin": false,
        "last_login": "0001-01-01T00:00:00Z",
        "created": "2026-02-19T20:16:44Z",
        "restricted": false,
        "active": false,
        "prohibit_login": false,
        "location": "",
        "website": "",
        "description": "",
        "visibility": "public",
        "followers_count": 0,
        "following_count": 0,
        "starred_repos_count": 0,
        "username": "abla-test"
      },
      "name": "abla-test-project",
      "full_name": "abla-test/abla-test-project",
      "description": "",
      "empty": true,
      "private": false,
      "fork": false,
      "template": false,
      "mirror": false,
      "size": 27,
      "language": "",
      "languages_url": "https://gitea.com/api/v1/repos/abla-test/abla-test-project/languages",
      "html_url": "https://gitea.com/abla-test/abla-test-project",
      "url": "https://gitea.com/api/v1/repos/abla-test/abla-test-project",
      "link": "",
      "ssh_url": "git@gitea.com:abla-test/abla-test-project.git",
      "clone_url": "https://gitea.com/abla-test/abla-test-project.git",
      "original_url": "https://gitlab.com/abla-test-group/abla-test-project.git",
      "website": "",
      "stars_count": 0,
      "forks_count": 0,
      "watchers_count": 1,
      "open_issues_count": 33,
      "open_pr_counter": 0,
      "release_counter": 0,
      "default_branch": "main",
      "archived": false,
      "created_at": "2026-02-19T20:25:29Z",
      "updated_at": "2026-02-19T20:27:08Z",
      "archived_at": "1970-01-01T00:00:00Z",
      "permissions": {
        "admin": false,
        "push": false,
        "pull": true
      },
      "has_code": true,
      "has_issues": true,
      "internal_tracker": {
        "enable_time_tracker": true,
        "allow_only_contributors_to_track_time": true,
        "enable_issue_dependencies": true
      },
      "has_wiki": true,
      "has_pull_requests": true,
      "has_projects": true,
      "projects_mode": "all",
      "has_releases": true,
      "has_packages": false,
      "has_actions": true,
      "ignore_whitespace_conflicts": false,
      "allow_merge_commits": true,
      "allow_rebase": true,
      "allow_rebase_explicit": true,
      "allow_squash_merge": true,
      "allow_fast_forward_only_merge": true,
      "allow_rebase_update": true,
      "allow_manual_merge": false,
      "autodetect_manual_merge": false,
      "default_delete_branch_after_merge": false,
      "default_merge_style": "merge",
      "default_allow_maintainer_edit": false,
      "avatar_url": "",
      "internal": false,
      "mirror_interval": "",
      "object_format_name": "sha1",
      "mirror_updated": "0001-01-01T00:00:00Z",
      "topics": [],
      "licenses": []
    },
    {
      "id": 17828,
      "owner": {
        "id": 17871,
        "login": "apachenpub",
        "login_name": "",
        "source_id": 0,
        "full_name": "",
        "email": "17871+apachenpub@noreply.gitea.com",
        "avatar_url": "https://seccdn.libravatar.org/avatar/b1585f683ed853daaaf491fab21e40d6?d=identicon",
        "html_url": "https://gitea.com/apachenpub",
        "language": "",
        "is_admin": false,
        "last_login": "0001-01-01T00:00:00Z",
        "created": "2021-02-03T12:21:59Z",
        "restricted": false,
        "active": false,
        "prohibit_login": false,
        "location": "",
        "website": "",
        "description": "",
        "visibility": "public",
        "followers_count": 0,
        "following_count": 0,
        "starred_repos_count": 0,
        "username": "apachenpub"
      },
      "name": "accept-test",
      "full_name": "apachenpub/accept-test",
      "description": "",
      "empty": false,
      "private": false,
      "fork": false,
      "template": false,
      "mirror": false,
      "size": 23,
      "language": "Python",
      "languages_url": "https://gitea.com/api/v1/repos/apachenpub/accept-test/languages",
      "html_url": "https://gitea.com/apachenpub/accept-test",
      "url": "https://gitea.com/api/v1/repos/apachenpub/accept-test",
      "link": "",
      "ssh_url": "git@gitea.com:apachenpub/accept-test.git",
      "clone_url": "https://gitea.com/apachenpub/accept-test.git",
      "original_url": "",
      "website": "",
      "stars_count": 0,
      "forks_count": 0,
      "watchers_count": 1,
      "open_issues_count": 0,
      "open_pr_counter": 1,
      "release_counter": 0,
      "default_branch": "master",
      "archived": false,
      "created_at": "2021-02-03T12:24:58Z",
      "updated_at": "2021-02-03T13:17:03Z",
      "archived_at": "1970-01-01T00:00:00Z",
      "permissions": {
        "admin": false,
        "push": false,
        "pull": true
      },
      "has_code": true,
      "has_issues": true,
      "internal_tracker": {
        "enable_time_tracker": true,
        "allow_only_contributors_to_track_time": true,
        "enable_issue_dependencies": true
      },
      "has_wiki": true,
      "has_pull_requests": true,
      "has_projects": true,
      "projects_mode": "",
      "has_releases": true,
      "has_packages": false,
      "has_actions": false,
      "ignore_whitespace_conflicts": false,
      "allow_merge_commits": true,
      "allow_rebase": true,
      "allow_rebase_explicit": true,
      "allow_squash_merge": true,
      "allow_fast_forward_only_merge": false,
      "allow_rebase_update": true,
      "allow_manual_merge": false,
      "autodetect_manual_merge": false,
      "default_delete_branch_after_merge": false,
      "default_merge_style": "merge",
      "default_allow_maintainer_edit": false,
      "avatar_url": "",
      "internal": false,
      "mirror_interval": "",
      "object_format_name": "sha1",
      "mirror_updated": "0001-01-01T00:00:00Z",
      "topics": [],
      "licenses": []
    },
    {
      "id": 16135,
      "owner": {
        "id": 2831,
        "login": "xingkong",
        "login_name": "",
        "source_id": 0,
        "full_name": "xingkong",
        "email": "2831+xingkong@noreply.gitea.com",
        "avatar_url": "https://seccdn.libravatar.org/avatar/1bca7f750627c91ea1026450c352906d?d=identicon",
        "html_url": "https://gitea.com/xingkong",
        "language": "",
        "is_admin": false,
        "last_login": "0001-01-01T00:00:00Z",
        "created": "2020-02-03T02:45:38Z",
        "restricted": false,
        "active": false,
        "prohibit_login": false,
        "location": "",
        "website": "",
        "description": "",
        "visibility": "public",
        "followers_count": 0,
        "following_count": 0,
        "starred_repos_count": 0,
        "username": "xingkong"
      },
      "name": "accessible-testing",
      "full_name": "xingkong/accessible-testing",
      "description": "Accessible test warehouse of gitea",
      "empty": true,
      "private": false,
      "fork": false,
      "template": false,
      "mirror": false,
      "size": 0,
      "language": "",
      "languages_url": "https://gitea.com/api/v1/repos/xingkong/accessible-testing/languages",
      "html_url": "https://gitea.com/xingkong/accessible-testing",
      "url": "https://gitea.com/api/v1/repos/xingkong/accessible-testing",
      "link": "",
      "ssh_url": "git@gitea.com:xingkong/accessible-testing.git",
      "clone_url": "https://gitea.com/xingkong/accessible-testing.git",
      "original_url": "",
      "website": "",
      "stars_count": 0,
      "forks_count": 0,
      "watchers_count": 1,
      "open_issues_count": 0,
      "open_pr_counter": 0,
      "release_counter": 0,
      "default_branch": "master",
      "archived": false,
      "created_at": "2020-08-17T02:00:31Z",
      "updated_at": "2020-08-17T02:00:31Z",
      "archived_at": "1970-01-01T00:00:00Z",
      "permissions": {
        "admin": false,
        "push": false,
        "pull": true
      },
      "has_code": true,
      "has_issues": true,
      "internal_tracker": {
        "enable_time_tracker": true,
        "allow_only_contributors_to_track_time": true,
        "enable_issue_dependencies": true
      },
      "has_wiki": true,
      "has_pull_requests": true,
      "has_projects": false,
      "projects_mode": "all",
      "has_releases": true,
      "has_packages": false,
      "has_actions": false,
      "ignore_whitespace_conflicts": false,
      "allow_merge_commits": true,
      "allow_rebase": true,
      "allow_rebase_explicit": true,
      "allow_squash_merge": true,
      "allow_fast_forward_only_merge": false,
      "allow_rebase_update": true,
      "allow_manual_merge": false,
      "autodetect_manual_merge": false,
      "default_delete_branch_after_merge": false,
      "default_merge_style": "merge",
      "default_allow_maintainer_edit": false,
      "avatar_url": "",
      "internal": false,
      "mirror_interval": "",
      "object_format_name": "sha1",
      "mirror_updated": "0001-01-01T00:00:00Z",
      "topics": [],
      "licenses": []
    },
    {
      "id": 60612,
      "owner": {
        "id": 73398,
        "login": "cloudchamb3r",
        "login_name": "",
        "source_id": 0,
        "full_name": "<img src='x' onerror=alert(1)/>",
        "email": "73398+cloudchamb3r@noreply.gitea.com",
        "avatar_url": "https://seccdn.libravatar.org/avatar/f267f75116baed4a0cf150615ff79972?d=identicon",
        "html_url": "https://gitea.com/cloudchamb3r",
        "language": "",
        "is_admin": false,
        "last_login": "0001-01-01T00:00:00Z",
        "created": "2024-07-04T17:40:58Z",
        "restricted": false,
        "active": false,
        "prohibit_login": false,
        "location": "",
        "website": "",
        "description": "",
        "visibility": "public",
        "followers_count": 0,
        "following_count": 0,
        "starred_repos_count": 0,
        "username": "cloudchamb3r"
      },
      "name": "acorn-final-be-test",
      "full_name": "cloudchamb3r/acorn-final-be-test",
      "description": "",
      "empty": false,
      "private": false,
      "fork": false,
      "template": false,
      "mirror": false,
      "size": 568,
      "language": "Java",
      "languages_url": "https://gitea.com/api/v1/repos/cloudchamb3r/acorn-final-be-test/languages",
      "html_url": "https://gitea.com/cloudchamb3r/acorn-final-be-test",
      "url": "https://gitea.com/api/v1/repos/cloudchamb3r/acorn-final-be-test",
      "link": "",
      "ssh_url": "git@gitea.com:cloudchamb3r/acorn-final-be-test.git",
      "clone_url": "https://gitea.com/cloudchamb3r/acorn-final-be-test.git",
      "original_url": "https://github.com/cloudchamb3r/acorn-final-be",
      "website": "",
      "stars_count": 0,
      "forks_count": 0,
      "watchers_count": 1,
      "open_issues_count": 6,
      "open_pr_counter": 0,
      "release_counter": 1,
      "default_branch": "main",
      "archived": false,
      "created_at": "2024-07-05T03:31:12Z",
      "updated_at": "2024-07-05T03:33:42Z",
      "archived_at": "1970-01-01T00:00:00Z",
      "permissions": {
        "admin": false,
        "push": false,
        "pull": true
      },
      "has_code": true,
      "has_issues": true,
      "internal_tracker": {
        "enable_time_tracker": true,
        "allow_only_contributors_to_track_time": true,
        "enable_issue_dependencies": true
      },
      "has_wiki": true,
      "has_pull_requests": true,
      "has_projects": true,
      "projects_mode": "all",
      "has_releases": true,
      "has_packages": false,
      "has_actions": true,
      "ignore_whitespace_conflicts": false,
      "allow_merge_commits": true,
      "allow_rebase": true,
      "allow_rebase_explicit": true,
      "allow_squash_merge": true,
      "allow_fast_forward_only_merge": true,
      "allow_rebase_update": true,
      "allow_manual_merge": false,
      "autodetect_manual_merge": false,
      "default_delete_branch_after_merge": false,
      "default_merge_style": "merge",
      "default_allow_maintainer_edit": false,
      "avatar_url": "",
      "internal": false,
      "mirror_interval": "",
      "object_format_name": "sha1",
      "mirror_updated": "0001-01-01T00:00:00Z",
      "topics": [],
      "licenses": []
    },
    {
      "id": 42637,
      "owner": {
        "id": 48897,
        "login": "Ferret",
        "login_name": "",
        "source_id": 0,
        "full_name": "",
        "email": "48897+ferret@noreply.gitea.com",
        "avatar_url": "https://seccdn.libravatar.org/avatar/c80bb5e6be8e3f892184a060105e94dc?d=identicon",
        "html_url": "https://gitea.com/Ferret",
        "language": "",
        "is_admin": false,
        "last_login": "0001-01-01T00:00:00Z",
        "created": "2023-09-11T11:58:05Z",
        "restricted": false,
        "active": false,
        "prohibit_login": false,
        "location": "",
        "website": "",
        "description": "",
        "visibility": "public",
        "followers_count": 0,
        "following_count": 0,
        "starred_repos_count": 1,
        "username": "Ferret"
      },
      "name": "action-test",
      "full_name": "Ferret/action-test",
      "description": "",
      "empty": true,
      "private": false,
      "fork": false,
      "template": false,
      "mirror": false,
      "size": 24,
      "language": "",
      "languages_url": "https://gitea.com/api/v1/repos/Ferret/action-test/languages",
      "html_url": "https://gitea.com/Ferret/action-test",
      "url": "https://gitea.com/api/v1/repos/Ferret/action-test",
      "link": "",
      "ssh_url": "git@gitea.com:Ferret/action-test.git",
      "clone_url": "https://gitea.com/Ferret/action-test.git",
      "original_url": "",
      "website": "",
      "stars_count": 0,
      "forks_count": 0,
      "watchers_count": 1,
      "open_issues_count": 0,
      "open_pr_counter": 0,
      "release_counter": 0,
      "default_branch": "main",
      "archived": false,
      "created_at": "2023-09-13T07:12:52Z",
      "updated_at": "2023-09-13T07:12:52Z",
      "archived_at": "1970-01-01T00:00:00Z",
      "permissions": {
        "admin": false,
        "push": false,
        "pull": true
      },
      "has_code": true,
      "has_issues": true,
      "internal_tracker": {
        "enable_time_tracker": true,
        "allow_only_contributors_to_track_time": true,
        "enable_issue_dependencies": true
      },
      "has_wiki": true,
      "has_pull_requests": true,
      "has_projects": true,
      "projects_mode": "",
      "has_releases": true,
      "has_packages": false,
      "has_actions": false,
      "ignore_whitespace_conflicts": false,
      "allow_merge_commits": true,
      "allow_rebase": true,
      "allow_rebase_explicit": true,
      "allow_squash_merge": true,
      "allow_fast_forward_only_merge": false,
      "allow_rebase_update": true,
      "allow_manual_merge": false,
      "autodetect_manual_merge": false,
      "default_delete_branch_after_merge": false,
      "default_merge_style": "merge",
      "default_allow_maintainer_edit": false,
      "avatar_url": "",
      "internal": false,
      "mirror_interval": "",
      "object_format_name": "sha1",
      "mirror_updated": "0001-01-01T00:00:00Z",
      "topics": [],
      "licenses": []
    }
  ]
  """

  @Test
  fun `deserializes repository list`() {
    val repos = deserialize(data, Array<GiteaRepositoryDTO>::class.java)
    assertNotNull(repos)
    assertEquals(10, repos!!.size)
    repos.forEachGuaranteed { assertNotNull(it) }
  }

  @Test
  fun `deserializes basic repository fields`() {
    val json = """
      {
        "id": 113277,
        "owner": {
          "id": 156110,
          "login": "evallab01"
        },
        "name": "my-repo",
        "full_name": "evallab01/my-repo",
        "description": "A test repository",
        "empty": true,
        "html_url": "https://gitea.com/evallab01/my-repo",
        "url": "https://gitea.com/api/v1/repos/evallab01/my-repo",
        "ssh_url": "git@gitea.com:evallab01/my-repo.git",
        "clone_url": "https://gitea.com/evallab01/my-repo.git",
        "original_url": "",
        "default_branch": "main",
        "created_at": "2025-11-08T12:25:30Z",
        "updated_at": "2025-11-08T12:25:30Z",
        "has_code": true,
        "has_issues": true,
        "has_pull_requests": true,
        "open_issues_count": 0,
        "open_pr_counter": 0,
        "allow_merge_commits": true,
        "allow_rebase": true,
        "allow_rebase_explicit": true,
        "allow_squash_merge": true,
        "allow_fast_forward_only_merge": true,
        "allow_rebase_update": true,
        "allow_manual_merge": false,
        "autodetect_manual_merge": false,
        "default_delete_branch_after_merge": false,
        "default_merge_style": "merge",
        "default_allow_maintainer_edit": false
      }
    """.trimIndent()

    val repo = deserialize(json, GiteaRepositoryDTO::class.java)
    assertNotNull(repo)
    assertEquals(113277, repo!!.id)
    assertEquals("my-repo", repo.name)
    assertEquals("evallab01/my-repo", repo.fullName)
    assertEquals("A test repository", repo.description)
    assertTrue(repo.empty)
    print(repo)
  }

  @Test
  fun `deserializes owner as nested GiteaUserDTO`() {
    val json = """
      {
        "id": 123346,
        "owner": {
          "id": 178984,
          "login": "abla-test",
          "full_name": "Alex Blais",
          "email": "178984+abla-test@noreply.gitea.com",
          "avatar_url": "https://seccdn.libravatar.org/avatar/df96c1dce88674ffd95266845576a560?d=identicon",
          "html_url": "https://gitea.com/abla-test"
        },
        "name": "abla-test-project",
        "full_name": "abla-test/abla-test-project",
        "description": "",
        "empty": true,
        "html_url": "https://gitea.com/abla-test/abla-test-project",
        "url": "https://gitea.com/api/v1/repos/abla-test/abla-test-project",
        "ssh_url": "git@gitea.com:abla-test/abla-test-project.git",
        "clone_url": "https://gitea.com/abla-test/abla-test-project.git",
        "original_url": "https://gitlab.com/abla-test-group/abla-test-project.git",
        "default_branch": "main",
        "created_at": "2026-02-19T20:25:29Z",
        "updated_at": "2026-02-19T20:27:08Z",
        "has_code": true,
        "has_issues": true,
        "has_pull_requests": true,
        "open_issues_count": 33,
        "open_pr_counter": 0,
        "allow_merge_commits": true,
        "allow_rebase": true,
        "allow_rebase_explicit": true,
        "allow_squash_merge": true,
        "allow_fast_forward_only_merge": true,
        "allow_rebase_update": true,
        "allow_manual_merge": false,
        "autodetect_manual_merge": false,
        "default_delete_branch_after_merge": false,
        "default_merge_style": "merge",
        "default_allow_maintainer_edit": false
      }
    """.trimIndent()

    val repo = deserialize(json, GiteaRepositoryDTO::class.java)
    assertNotNull(repo)
    assertNotNull(repo!!.owner)
    assertEquals(178984, repo.owner.id)
    assertEquals("abla-test", repo.owner.login)
    assertEquals("Alex Blais", repo.owner.fullName)
    assertEquals("178984+abla-test@noreply.gitea.com", repo.owner.email)
    assertEquals("https://seccdn.libravatar.org/avatar/df96c1dce88674ffd95266845576a560?d=identicon", repo.owner.avatarUrl)
    assertEquals("https://gitea.com/abla-test", repo.owner.htmlUrl)
    print(repo)
  }

  @Test
  fun `deserializes snake_case html_url to htmlUrl`() {
    val json = """
      {
        "id": 1,
        "owner": { "id": 1, "login": "user" },
        "name": "repo",
        "full_name": "user/repo",
        "description": "",
        "empty": false,
        "html_url": "https://gitea.com/user/repo",
        "url": "https://gitea.com/api/v1/repos/user/repo",
        "ssh_url": "git@gitea.com:user/repo.git",
        "clone_url": "https://gitea.com/user/repo.git",
        "original_url": "",
        "default_branch": "main",
        "created_at": "2025-01-01T00:00:00Z",
        "has_code": true,
        "has_issues": true,
        "has_pull_requests": true,
        "open_issues_count": 0,
        "open_pr_counter": 0,
        "allow_merge_commits": true,
        "allow_rebase": true,
        "allow_rebase_explicit": true,
        "allow_squash_merge": true,
        "allow_fast_forward_only_merge": false,
        "allow_rebase_update": true,
        "allow_manual_merge": false,
        "autodetect_manual_merge": false,
        "default_delete_branch_after_merge": false,
        "default_merge_style": "merge",
        "default_allow_maintainer_edit": false
      }
    """.trimIndent()

    val repo = deserialize(json, GiteaRepositoryDTO::class.java)
    assertNotNull(repo)
    assertEquals("https://gitea.com/user/repo", repo!!.htmlUrl)
    print(repo)
  }

  @Test
  fun `deserializes snake_case ssh_url to sshUrl`() {
    val json = """
      {
        "id": 1,
        "owner": { "id": 1, "login": "user" },
        "name": "repo",
        "full_name": "user/repo",
        "description": "",
        "empty": false,
        "html_url": "https://gitea.com/user/repo",
        "url": "https://gitea.com/api/v1/repos/user/repo",
        "ssh_url": "git@gitea.com:user/repo.git",
        "clone_url": "https://gitea.com/user/repo.git",
        "original_url": "",
        "default_branch": "main",
        "created_at": "2025-01-01T00:00:00Z",
        "has_code": true,
        "has_issues": true,
        "has_pull_requests": true,
        "open_issues_count": 0,
        "open_pr_counter": 0,
        "allow_merge_commits": true,
        "allow_rebase": true,
        "allow_rebase_explicit": true,
        "allow_squash_merge": true,
        "allow_fast_forward_only_merge": false,
        "allow_rebase_update": true,
        "allow_manual_merge": false,
        "autodetect_manual_merge": false,
        "default_delete_branch_after_merge": false,
        "default_merge_style": "merge",
        "default_allow_maintainer_edit": false
      }
    """.trimIndent()

    val repo = deserialize(json, GiteaRepositoryDTO::class.java)
    assertNotNull(repo)
    assertEquals("git@gitea.com:user/repo.git", repo!!.sshUrl)
    print(repo)
  }

  @Test
  fun `deserializes snake_case clone_url to cloneUrl`() {
    val json = """
      {
        "id": 1,
        "owner": { "id": 1, "login": "user" },
        "name": "repo",
        "full_name": "user/repo",
        "description": "",
        "empty": false,
        "html_url": "https://gitea.com/user/repo",
        "url": "https://gitea.com/api/v1/repos/user/repo",
        "ssh_url": "git@gitea.com:user/repo.git",
        "clone_url": "https://gitea.com/user/repo.git",
        "original_url": "",
        "default_branch": "main",
        "created_at": "2025-01-01T00:00:00Z",
        "has_code": true,
        "has_issues": true,
        "has_pull_requests": true,
        "open_issues_count": 0,
        "open_pr_counter": 0,
        "allow_merge_commits": true,
        "allow_rebase": true,
        "allow_rebase_explicit": true,
        "allow_squash_merge": true,
        "allow_fast_forward_only_merge": false,
        "allow_rebase_update": true,
        "allow_manual_merge": false,
        "autodetect_manual_merge": false,
        "default_delete_branch_after_merge": false,
        "default_merge_style": "merge",
        "default_allow_maintainer_edit": false
      }
    """.trimIndent()

    val repo = deserialize(json, GiteaRepositoryDTO::class.java)
    assertNotNull(repo)
    assertEquals("https://gitea.com/user/repo.git", repo!!.cloneUrl)
    print(repo)
  }

  @Test
  fun `deserializes created_at date field in utc`() {
    val json = """
      {
        "id": 1,
        "owner": { "id": 1, "login": "user" },
        "name": "repo",
        "full_name": "user/repo",
        "description": "",
        "empty": false,
        "html_url": "https://gitea.com/user/repo",
        "url": "https://gitea.com/api/v1/repos/user/repo",
        "ssh_url": "git@gitea.com:user/repo.git",
        "clone_url": "https://gitea.com/user/repo.git",
        "original_url": "",
        "default_branch": "main",
        "created_at": "2025-11-08T12:25:30Z",
        "updated_at": "2025-11-08T12:25:30Z",
        "has_code": true,
        "has_issues": true,
        "has_pull_requests": true,
        "open_issues_count": 0,
        "open_pr_counter": 0,
        "allow_merge_commits": true,
        "allow_rebase": true,
        "allow_rebase_explicit": true,
        "allow_squash_merge": true,
        "allow_fast_forward_only_merge": false,
        "allow_rebase_update": true,
        "allow_manual_merge": false,
        "autodetect_manual_merge": false,
        "default_delete_branch_after_merge": false,
        "default_merge_style": "merge",
        "default_allow_maintainer_edit": false
      }
    """.trimIndent()

    val repo = deserialize(json, GiteaRepositoryDTO::class.java)
    assertNotNull(repo)
    assertNotNull(repo!!.createdAt)
    // 2025-11-08T12:25:30Z epoch millis
    assertEquals(1762604730000L, repo.createdAt.time)
    print(repo.createdAt)
  }

  @Test
  fun `deserializes updated_at as nullable date`() {
    val json = """
      {
        "id": 1,
        "owner": { "id": 1, "login": "user" },
        "name": "repo",
        "full_name": "user/repo",
        "description": "",
        "empty": false,
        "html_url": "https://gitea.com/user/repo",
        "url": "https://gitea.com/api/v1/repos/user/repo",
        "ssh_url": "git@gitea.com:user/repo.git",
        "clone_url": "https://gitea.com/user/repo.git",
        "original_url": "",
        "default_branch": "main",
        "created_at": "2025-01-15T12:00:33Z",
        "has_code": true,
        "has_issues": true,
        "has_pull_requests": true,
        "open_issues_count": 0,
        "open_pr_counter": 0,
        "allow_merge_commits": true,
        "allow_rebase": true,
        "allow_rebase_explicit": true,
        "allow_squash_merge": true,
        "allow_fast_forward_only_merge": false,
        "allow_rebase_update": true,
        "allow_manual_merge": false,
        "autodetect_manual_merge": false,
        "default_delete_branch_after_merge": false,
        "default_merge_style": "merge",
        "default_allow_maintainer_edit": false
      }
    """.trimIndent()

    val repo = deserialize(json, GiteaRepositoryDTO::class.java)
    assertNotNull(repo)
    assertNull(repo!!.updatedAt)
    print(repo)
  }

  @Test
  fun `deserializes boolean merge strategy fields`() {
    val json = """
      {
        "id": 45897,
        "owner": { "id": 54240, "login": "AadheshAero" },
        "name": "Aadheshtest",
        "full_name": "AadheshAero/Aadheshtest",
        "description": "",
        "empty": false,
        "html_url": "https://gitea.com/AadheshAero/Aadheshtest",
        "url": "https://gitea.com/api/v1/repos/AadheshAero/Aadheshtest",
        "ssh_url": "git@gitea.com:AadheshAero/Aadheshtest.git",
        "clone_url": "https://gitea.com/AadheshAero/Aadheshtest.git",
        "original_url": "",
        "default_branch": "master",
        "created_at": "2023-11-22T18:36:01Z",
        "updated_at": "2023-11-22T18:42:46Z",
        "has_code": true,
        "has_issues": true,
        "has_pull_requests": true,
        "open_issues_count": 0,
        "open_pr_counter": 0,
        "allow_merge_commits": true,
        "allow_rebase": true,
        "allow_rebase_explicit": true,
        "allow_squash_merge": true,
        "allow_fast_forward_only_merge": false,
        "allow_rebase_update": true,
        "allow_manual_merge": false,
        "autodetect_manual_merge": false,
        "default_delete_branch_after_merge": false,
        "default_merge_style": "merge",
        "default_allow_maintainer_edit": false
      }
    """.trimIndent()

    val repo = deserialize(json, GiteaRepositoryDTO::class.java)
    assertNotNull(repo)
    assertTrue(repo!!.allowMergeCommits)
    assertTrue(repo.allowRebase)
    assertTrue(repo.allowRebaseExplicit)
    assertTrue(repo.allowSquashMerge)
    assertFalse(repo.allowFastForwardOnlyMerge)
    assertTrue(repo.allowRebaseUpdate)
    assertFalse(repo.allowManualMerge)
    assertFalse(repo.autodetectManualMerge)
    assertFalse(repo.defaultDeleteBranchAfterMerge)
    assertEquals("merge", repo.defaultMergeStyle)
    assertFalse(repo.defaultAllowMaintainerEdit)
    print(repo)
  }

  @Test
  fun `deserializes issue and pr count fields`() {
    val json = """
      {
        "id": 17828,
        "owner": { "id": 17871, "login": "apachenpub" },
        "name": "accept-test",
        "full_name": "apachenpub/accept-test",
        "description": "",
        "empty": false,
        "html_url": "https://gitea.com/apachenpub/accept-test",
        "url": "https://gitea.com/api/v1/repos/apachenpub/accept-test",
        "ssh_url": "git@gitea.com:apachenpub/accept-test.git",
        "clone_url": "https://gitea.com/apachenpub/accept-test.git",
        "original_url": "",
        "default_branch": "master",
        "created_at": "2021-02-03T12:24:58Z",
        "updated_at": "2021-02-03T13:17:03Z",
        "has_code": true,
        "has_issues": true,
        "has_pull_requests": true,
        "open_issues_count": 0,
        "open_pr_counter": 1,
        "allow_merge_commits": true,
        "allow_rebase": true,
        "allow_rebase_explicit": true,
        "allow_squash_merge": true,
        "allow_fast_forward_only_merge": false,
        "allow_rebase_update": true,
        "allow_manual_merge": false,
        "autodetect_manual_merge": false,
        "default_delete_branch_after_merge": false,
        "default_merge_style": "merge",
        "default_allow_maintainer_edit": false
      }
    """.trimIndent()

    val repo = deserialize(json, GiteaRepositoryDTO::class.java)
    assertNotNull(repo)
    assertEquals(0, repo!!.openIssuesCount)
    assertEquals(1, repo.openPrCounter)
    print(repo)
  }

  @Test
  fun `deserializes original_url for mirrored repo`() {
    val json = """
      {
        "id": 60612,
        "owner": { "id": 73398, "login": "cloudchamb3r" },
        "name": "acorn-final-be-test",
        "full_name": "cloudchamb3r/acorn-final-be-test",
        "description": "",
        "empty": false,
        "html_url": "https://gitea.com/cloudchamb3r/acorn-final-be-test",
        "url": "https://gitea.com/api/v1/repos/cloudchamb3r/acorn-final-be-test",
        "ssh_url": "git@gitea.com:cloudchamb3r/acorn-final-be-test.git",
        "clone_url": "https://gitea.com/cloudchamb3r/acorn-final-be-test.git",
        "original_url": "https://github.com/cloudchamb3r/acorn-final-be",
        "default_branch": "main",
        "created_at": "2024-07-05T03:31:12Z",
        "updated_at": "2024-07-05T03:33:42Z",
        "has_code": true,
        "has_issues": true,
        "has_pull_requests": true,
        "open_issues_count": 6,
        "open_pr_counter": 0,
        "allow_merge_commits": true,
        "allow_rebase": true,
        "allow_rebase_explicit": true,
        "allow_squash_merge": true,
        "allow_fast_forward_only_merge": true,
        "allow_rebase_update": true,
        "allow_manual_merge": false,
        "autodetect_manual_merge": false,
        "default_delete_branch_after_merge": false,
        "default_merge_style": "merge",
        "default_allow_maintainer_edit": false
      }
    """.trimIndent()

    val repo = deserialize(json, GiteaRepositoryDTO::class.java)
    assertNotNull(repo)
    assertEquals("https://github.com/cloudchamb3r/acorn-final-be", repo!!.originalUrl)
    print(repo)
  }

  @Test
  fun `deserializes full repository response matching gitea api format`() {
    // Matches the Gitea API /api/v1/repos/search response format (single repo from sample data)
    val json = """
      {
        "id": 123346,
        "owner": {
          "id": 178984,
          "login": "abla-test",
          "full_name": "Alex Blais",
          "email": "178984+abla-test@noreply.gitea.com",
          "avatar_url": "https://seccdn.libravatar.org/avatar/df96c1dce88674ffd95266845576a560?d=identicon",
          "html_url": "https://gitea.com/abla-test"
        },
        "name": "abla-test-project",
        "full_name": "abla-test/abla-test-project",
        "description": "",
        "empty": true,
        "html_url": "https://gitea.com/abla-test/abla-test-project",
        "url": "https://gitea.com/api/v1/repos/abla-test/abla-test-project",
        "ssh_url": "git@gitea.com:abla-test/abla-test-project.git",
        "clone_url": "https://gitea.com/abla-test/abla-test-project.git",
        "original_url": "https://gitlab.com/abla-test-group/abla-test-project.git",
        "default_branch": "main",
        "created_at": "2026-02-19T20:25:29Z",
        "updated_at": "2026-02-19T20:27:08Z",
        "has_code": true,
        "has_issues": true,
        "has_pull_requests": true,
        "open_issues_count": 33,
        "open_pr_counter": 0,
        "allow_merge_commits": true,
        "allow_rebase": true,
        "allow_rebase_explicit": true,
        "allow_squash_merge": true,
        "allow_fast_forward_only_merge": true,
        "allow_rebase_update": true,
        "allow_manual_merge": false,
        "autodetect_manual_merge": false,
        "default_delete_branch_after_merge": false,
        "default_merge_style": "merge",
        "default_allow_maintainer_edit": false
      }
    """.trimIndent()

    val dto = deserialize(json, GiteaRepositoryDTO::class.java)
    assertNotNull(dto)
    assertEquals(123346, dto!!.id)
    assertEquals("abla-test-project", dto.name)
    assertEquals("abla-test/abla-test-project", dto.fullName)
    assertEquals("", dto.description)
    assertTrue(dto.empty)
    assertEquals("https://gitea.com/abla-test/abla-test-project", dto.htmlUrl)
    assertEquals("https://gitea.com/api/v1/repos/abla-test/abla-test-project", dto.url)
    assertEquals("git@gitea.com:abla-test/abla-test-project.git", dto.sshUrl)
    assertEquals("https://gitea.com/abla-test/abla-test-project.git", dto.cloneUrl)
    assertEquals("https://gitlab.com/abla-test-group/abla-test-project.git", dto.originalUrl)
    assertEquals("main", dto.defaultBranch)
    assertNotNull(dto.createdAt)
    assertNotNull(dto.updatedAt)
    assertTrue(dto.hasCode)
    assertTrue(dto.hasIssues)
    assertTrue(dto.hasPullRequests)
    assertEquals(33, dto.openIssuesCount)
    assertEquals(0, dto.openPrCounter)
    assertTrue(dto.allowMergeCommits)
    assertTrue(dto.allowRebase)
    assertTrue(dto.allowRebaseExplicit)
    assertTrue(dto.allowSquashMerge)
    assertTrue(dto.allowFastForwardOnlyMerge)
    assertTrue(dto.allowRebaseUpdate)
    assertFalse(dto.allowManualMerge)
    assertFalse(dto.autodetectManualMerge)
    assertFalse(dto.defaultDeleteBranchAfterMerge)
    assertEquals("merge", dto.defaultMergeStyle)
    assertFalse(dto.defaultAllowMaintainerEdit)

    // Verify nested owner
    assertEquals(178984, dto.owner.id)
    assertEquals("abla-test", dto.owner.login)
    print(dto)
  }

  @Test
  fun `unknown fields are ignored`() {
    // FAIL_ON_UNKNOWN_PROPERTIES is false — unknown fields must be silently ignored
    val json = """
      {
        "id": 1,
        "owner": { "id": 1, "login": "user" },
        "name": "repo",
        "full_name": "user/repo",
        "description": "",
        "empty": false,
        "html_url": "https://gitea.com/user/repo",
        "url": "https://gitea.com/api/v1/repos/user/repo",
        "ssh_url": "git@gitea.com:user/repo.git",
        "clone_url": "https://gitea.com/user/repo.git",
        "original_url": "",
        "default_branch": "main",
        "created_at": "2025-01-01T00:00:00Z",
        "has_code": true,
        "has_issues": true,
        "has_pull_requests": true,
        "open_issues_count": 0,
        "open_pr_counter": 0,
        "allow_merge_commits": true,
        "allow_rebase": true,
        "allow_rebase_explicit": true,
        "allow_squash_merge": true,
        "allow_fast_forward_only_merge": false,
        "allow_rebase_update": true,
        "allow_manual_merge": false,
        "autodetect_manual_merge": false,
        "default_delete_branch_after_merge": false,
        "default_merge_style": "merge",
        "default_allow_maintainer_edit": false,
        "archived": false,
        "private": false,
        "fork": false,
        "template": false,
        "mirror": false,
        "size": 25,
        "language": "Java",
        "stars_count": 0,
        "forks_count": 0,
        "watchers_count": 1,
        "unknown_future_field": "some_value",
        "another_unknown": 42
      }
    """.trimIndent()

    val repo = deserialize(json, GiteaRepositoryDTO::class.java)
    assertNotNull(repo)
    assertEquals(1, repo!!.id)
    assertEquals("repo", repo.name)
    print(repo)
  }

  @Test
  fun `deserializes repository with empty default_branch`() {
    val json = """
      {
        "id": 86444,
        "owner": { "id": 100043, "login": "egorlavrinovich" },
        "name": ".test",
        "full_name": "egorlavrinovich/.test",
        "description": "",
        "empty": true,
        "html_url": "https://gitea.com/egorlavrinovich/.test",
        "url": "https://gitea.com/api/v1/repos/egorlavrinovich/.test",
        "ssh_url": "git@gitea.com:egorlavrinovich/.test.git",
        "clone_url": "https://gitea.com/egorlavrinovich/.test.git",
        "original_url": "",
        "default_branch": "",
        "created_at": "2025-01-15T12:00:33Z",
        "updated_at": "2025-02-03T13:07:14Z",
        "has_code": true,
        "has_issues": true,
        "has_pull_requests": true,
        "open_issues_count": 2,
        "open_pr_counter": 0,
        "allow_merge_commits": true,
        "allow_rebase": true,
        "allow_rebase_explicit": true,
        "allow_squash_merge": true,
        "allow_fast_forward_only_merge": true,
        "allow_rebase_update": true,
        "allow_manual_merge": false,
        "autodetect_manual_merge": false,
        "default_delete_branch_after_merge": false,
        "default_merge_style": "merge",
        "default_allow_maintainer_edit": false
      }
    """.trimIndent()

    val repo = deserialize(json, GiteaRepositoryDTO::class.java)
    assertNotNull(repo)
    assertEquals("", repo!!.defaultBranch)
    assertTrue(repo.empty)
    assertEquals(2, repo.openIssuesCount)
    print(repo)
  }
}