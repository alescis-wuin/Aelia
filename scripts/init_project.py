#!/usr/bin/env python3
"""Project metadata, Git and GitHub bootstrap utility."""

from __future__ import annotations

import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

OLD_OWNER = "seynax"
OLD_REPO = "Aeliea"
OLD_DISPLAY_NAME = "Aelia"
OLD_GROUP_ID = "fr.seynax"
OLD_BASE_PACKAGE = "fr.seynax.aelia"
OLD_BASE_PACKAGE_PATH = OLD_BASE_PACKAGE.replace(".", "/")
OLD_DESCRIPTION = "Aelia is a Weather utility application"

TOPICS = ["java", "javafx", "maven", "weather", "mvc", "atlantafx", "desktop-app"]
TEXT_EXTENSIONS = {
    ".java", ".xml", ".md", ".txt", ".yml", ".yaml", ".css", ".properties", ".sh", ".py",
    ".gitignore", ".gitattributes", ".editorconfig", "", ".mf"
}
SKIPPED_DIRS = {".git", "target", "build", "out", "dist", ".idea", ".vscode"}


@dataclass(frozen=True)
class ProjectConfig:
    owner: str
    repo: str
    display_name: str
    group_id: str
    base_package: str
    description: str
    visibility: str
    create_remote: bool
    protect_branches: bool
    run_maven_verify: bool
    dry_run: bool


class BootstrapError(RuntimeError):
    """Raised when bootstrap cannot safely continue."""


def main(argv: list[str]) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)
    project_root = Path(__file__).resolve().parents[1]

    try:
        config = collect_config(args)
        print_config(config)
        if config.dry_run:
            print("Dry run enabled: no file, Git or GitHub change will be performed.")
            return 0

        customize_project(project_root, config)
        if config.run_maven_verify:
            run(["mvn", "verify"], project_root)
        initialize_git(project_root)

        if config.create_remote:
            initialize_github(project_root, config)
        else:
            print("Remote GitHub bootstrap skipped.")

        print("Project bootstrap completed.")
        return 0
    except BootstrapError as error:
        print(f"error: {error}", file=sys.stderr)
        return 2
    except subprocess.CalledProcessError as error:
        print(f"error: command failed with exit code {error.returncode}: {' '.join(error.cmd)}", file=sys.stderr)
        return error.returncode


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Configure Maven metadata and bootstrap Git/GitHub.")
    parser.add_argument("--non-interactive", action="store_true", help="Use provided flags and defaults without prompts.")
    parser.add_argument("--owner", help="GitHub owner or organization.")
    parser.add_argument("--repo", help="Repository and Maven artifact name.")
    parser.add_argument("--display-name", help="Human-readable project name.")
    parser.add_argument("--group-id", help="Maven groupId.")
    parser.add_argument("--base-package", help="Base Java package.")
    parser.add_argument("--description", help="Repository and Maven description.")
    parser.add_argument("--visibility", choices=("private", "public", "internal"), help="GitHub repository visibility.")
    parser.add_argument("--skip-remote", action="store_true", help="Do not create or configure a GitHub remote repository.")
    parser.add_argument("--skip-protection", action="store_true", help="Do not apply branch protection rules.")
    parser.add_argument("--skip-maven-verify", action="store_true", help="Do not run mvn verify before the first commit.")
    parser.add_argument("--dry-run", action="store_true", help="Print resolved values without changing files.")
    return parser


def collect_config(args: argparse.Namespace) -> ProjectConfig:
    owner_default = args.owner or detect_github_owner() or OLD_OWNER
    repo_default = args.repo or OLD_REPO
    display_default = args.display_name or title_from_repo(repo_default)
    group_default = args.group_id or f"io.github.{normalize_package_segment(owner_default)}"
    package_default = args.base_package or f"{group_default}.{normalize_package_segment(repo_default)}"
    description_default = args.description or OLD_DESCRIPTION
    visibility_default = args.visibility or "private"

    if args.non_interactive:
        config = ProjectConfig(
            owner=owner_default,
            repo=repo_default,
            display_name=display_default,
            group_id=group_default,
            base_package=package_default,
            description=description_default,
            visibility=visibility_default,
            create_remote=not args.skip_remote,
            protect_branches=not args.skip_protection,
            run_maven_verify=not args.skip_maven_verify,
            dry_run=args.dry_run,
        )
    else:
        config = ProjectConfig(
            owner=prompt("GitHub owner or organization", owner_default),
            repo=prompt("Repository name", repo_default),
            display_name=prompt("Project display name", display_default),
            group_id=prompt("Maven groupId", group_default),
            base_package=prompt("Base Java package", package_default),
            description=prompt("Repository description", description_default),
            visibility=prompt_choice("Repository visibility", visibility_default, ("private", "public", "internal")),
            create_remote=prompt_bool("Create/configure GitHub remote", not args.skip_remote),
            protect_branches=prompt_bool("Protect main, testing and develop", not args.skip_protection),
            run_maven_verify=prompt_bool("Run mvn verify before the first commit", not args.skip_maven_verify),
            dry_run=args.dry_run,
        )

    validate_config(config)
    if not config.create_remote:
        return ProjectConfig(**{**config.__dict__, "protect_branches": False})
    return config


def print_config(config: ProjectConfig) -> None:
    print("Resolved project configuration:")
    print(f"  owner:            {config.owner}")
    print(f"  repository:       {config.repo}")
    print(f"  display name:     {config.display_name}")
    print(f"  groupId:          {config.group_id}")
    print(f"  base package:     {config.base_package}")
    print(f"  description:      {config.description}")
    print(f"  visibility:       {config.visibility}")
    print(f"  create remote:    {config.create_remote}")
    print(f"  protect branches: {config.protect_branches}")


def customize_project(project_root: Path, config: ProjectConfig) -> None:
    replacements = [
        ("https://github.com/seynax/Aeliea", f"https://github.com/{config.owner}/{config.repo}"),
        ("git@github.com:seynax/Aeliea.git", f"git@github.com:{config.owner}/{config.repo}.git"),
        (OLD_BASE_PACKAGE_PATH, config.base_package.replace(".", "/")),
        (OLD_BASE_PACKAGE, config.base_package),
        (OLD_GROUP_ID, config.group_id),
        (OLD_DISPLAY_NAME, config.display_name),
        (OLD_DESCRIPTION, config.description),
        (OLD_REPO, config.repo),
        (OLD_OWNER, config.owner),
    ]

    move_package_directories(project_root, config.base_package)
    for path in iter_text_files(project_root):
        content = path.read_text(encoding="utf-8")
        updated = content
        for old, new in replacements:
            updated = updated.replace(old, new)
        if updated != content:
            path.write_text(updated, encoding="utf-8")
            print(f"updated {path.relative_to(project_root)}")


def move_package_directories(project_root: Path, base_package: str) -> None:
    new_package_path = base_package.replace(".", "/")
    for source_root in ("src/main/java", "src/test/java", "src/main/resources"):
        old_dir = project_root / source_root / OLD_BASE_PACKAGE_PATH
        new_dir = project_root / source_root / new_package_path
        if not old_dir.exists() or old_dir == new_dir:
            continue
        if new_dir.exists():
            raise BootstrapError(f"Target package directory already exists: {new_dir}")
        new_dir.parent.mkdir(parents=True, exist_ok=True)
        shutil.move(str(old_dir), str(new_dir))
        prune_empty_parents(old_dir.parent, project_root / source_root)
        print(f"moved {source_root}/{OLD_BASE_PACKAGE_PATH} -> {source_root}/{new_package_path}")


def prune_empty_parents(path: Path, stop: Path) -> None:
    current = path
    while current != stop and current.exists():
        try:
            current.rmdir()
        except OSError:
            break
        current = current.parent


def initialize_git(project_root: Path) -> None:
    require_command("git")
    if not (project_root / ".git").exists():
        run(["git", "init", "-b", "main"], project_root)
    else:
        run(["git", "branch", "-M", "main"], project_root)

    run(["git", "add", "."], project_root)
    has_staged_changes = subprocess.run(["git", "diff", "--cached", "--quiet"], cwd=project_root).returncode != 0
    if has_staged_changes:
        run(["git", "commit", "-m", "Initial project setup"], project_root)
    else:
        print("No staged changes to commit.")


def initialize_github(project_root: Path, config: ProjectConfig) -> None:
    require_command("gh")
    run(["gh", "auth", "status"], project_root)

    full_repo = f"{config.owner}/{config.repo}"
    repository_exists = subprocess.run(
        ["gh", "repo", "view", full_repo], cwd=project_root, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL
    ).returncode == 0

    visibility_flag = f"--{config.visibility}"
    if repository_exists:
        print(f"GitHub repository already exists: {full_repo}")
        ensure_origin(project_root, full_repo)
    else:
        run([
            "gh", "repo", "create", full_repo,
            visibility_flag,
            "--source", str(project_root),
            "--remote", "origin",
            "--description", config.description,
        ], project_root)
        ensure_origin(project_root, full_repo)

    run(["git", "switch", "main"], project_root)
    run(["git", "push", "-u", "origin", "main"], project_root)
    create_and_push_branch(project_root, "testing")
    create_and_push_branch(project_root, "develop")

    edit_repository_metadata(project_root, config)

    if config.protect_branches:
        for branch in ("main", "testing", "develop"):
            protect_branch(project_root, config.owner, config.repo, branch)
    run(["git", "switch", "develop"], project_root)


def ensure_origin(project_root: Path, full_repo: str) -> None:
    ssh_url = f"git@github.com:{full_repo}.git"
    has_origin = subprocess.run(
        ["git", "remote", "get-url", "origin"], cwd=project_root, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL
    ).returncode == 0
    if has_origin:
        run(["git", "remote", "set-url", "origin", ssh_url], project_root)
    else:
        run(["git", "remote", "add", "origin", ssh_url], project_root)


def create_and_push_branch(project_root: Path, branch: str) -> None:
    run(["git", "branch", "-f", branch, "main"], project_root)
    run(["git", "push", "-u", "origin", branch], project_root)


def edit_repository_metadata(project_root: Path, config: ProjectConfig) -> None:
    command = [
        "gh", "repo", "edit", f"{config.owner}/{config.repo}",
        "--description", config.description,
        "--default-branch", "develop",
        "--delete-branch-on-merge",
        "--enable-issues",
        "--enable-squash-merge",
        "--enable-rebase-merge",
    ]
    for topic in TOPICS:
        command.extend(["--add-topic", topic])
    run(command, project_root)


def protect_branch(project_root: Path, owner: str, repo: str, branch: str) -> None:
    payload = {
        "required_status_checks": None,
        "enforce_admins": True,
        "required_pull_request_reviews": {
            "dismiss_stale_reviews": True,
            "require_code_owner_reviews": False,
            "required_approving_review_count": 1,
            "require_last_push_approval": False,
        },
        "restrictions": None,
        "required_linear_history": True,
        "allow_force_pushes": False,
        "allow_deletions": False,
        "block_creations": False,
        "required_conversation_resolution": True,
        "lock_branch": False,
        "allow_fork_syncing": True,
    }
    with tempfile.NamedTemporaryFile("w", encoding="utf-8", suffix=".json", delete=False) as handle:
        json.dump(payload, handle)
        payload_path = handle.name
    try:
        run([
            "gh", "api", "-X", "PUT",
            f"repos/{owner}/{repo}/branches/{branch}/protection",
            "--input", payload_path,
        ], project_root)
        print(f"protected branch {branch}")
    except subprocess.CalledProcessError as error:
        print(
            "warning: branch protection failed for "
            f"{branch}. This can happen without admin permissions or with private repositories on plans "
            "that do not include protected branches.",
            file=sys.stderr,
        )
        if error.returncode not in (1, 4):
            raise
    finally:
        os.unlink(payload_path)


def iter_text_files(project_root: Path) -> Iterable[Path]:
    for path in project_root.rglob("*"):
        if not path.is_file():
            continue
        if any(part in SKIPPED_DIRS for part in path.parts):
            continue
        if path.suffix in TEXT_EXTENSIONS or path.name in TEXT_EXTENSIONS:
            try:
                path.read_text(encoding="utf-8")
            except UnicodeDecodeError:
                continue
            yield path


def run(command: list[str], cwd: Path) -> None:
    print("$ " + " ".join(command))
    subprocess.run(command, cwd=cwd, check=True)


def require_command(command_name: str) -> None:
    if shutil.which(command_name) is None:
        raise BootstrapError(f"Required command not found: {command_name}")


def detect_github_owner() -> str | None:
    if shutil.which("gh") is None:
        return None
    try:
        result = subprocess.run(
            ["gh", "api", "user", "--jq", ".login"],
            check=True,
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.DEVNULL,
        )
    except subprocess.CalledProcessError:
        return None
    detected = result.stdout.strip()
    return detected or None


def prompt(label: str, default: str) -> str:
    value = input(f"{label} [{default}]: ").strip()
    return value or default


def prompt_choice(label: str, default: str, choices: tuple[str, ...]) -> str:
    while True:
        value = prompt(label + " " + "/".join(choices), default)
        if value in choices:
            return value
        print(f"Allowed values: {', '.join(choices)}")


def prompt_bool(label: str, default: bool) -> bool:
    suffix = "Y/n" if default else "y/N"
    while True:
        value = input(f"{label} [{suffix}]: ").strip().lower()
        if not value:
            return default
        if value in {"y", "yes", "o", "oui"}:
            return True
        if value in {"n", "no", "non"}:
            return False
        print("Please answer yes or no.")


def validate_config(config: ProjectConfig) -> None:
    if not re.fullmatch(r"[A-Za-z0-9_.-]+", config.owner):
        raise BootstrapError("GitHub owner contains unsupported characters.")
    if not re.fullmatch(r"[A-Za-z0-9_.-]+", config.repo):
        raise BootstrapError("Repository name contains unsupported characters.")
    if not is_valid_package(config.group_id):
        raise BootstrapError("Maven groupId must also be a valid Java-style package.")
    if not is_valid_package(config.base_package):
        raise BootstrapError("Base package is not a valid Java package.")
    if not config.display_name.strip():
        raise BootstrapError("Display name must not be blank.")
    if not config.description.strip():
        raise BootstrapError("Description must not be blank.")


def is_valid_package(value: str) -> bool:
    identifier = r"[A-Za-z_$][A-Za-z0-9_$]*"
    return re.fullmatch(identifier + r"(\." + identifier + r")+", value) is not None


def normalize_package_segment(value: str) -> str:
    value = value.lower()
    value = re.sub(r"[-.]+", "", value)
    value = re.sub(r"[^a-z0-9_]", "_", value)
    value = re.sub(r"_+", "_", value).strip("_")
    if not value:
        value = "app"
    if value[0].isdigit():
        value = "app" + value
    return value


def title_from_repo(repo: str) -> str:
    words = []
    for part in re.split(r"[-_.]+", repo):
        if not part:
            continue
        words.append(part.upper() if part.lower() in {"api", "mvc", "ui", "ux"} else part.capitalize())
    return " ".join(words) or OLD_DISPLAY_NAME


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
