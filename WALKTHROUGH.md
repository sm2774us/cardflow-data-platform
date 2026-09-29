# CardFlow: Beginner Walkthrough (Windows 11)

Goal: get this project running on your PC, publish it to your own GitHub, and
protect `main` so **nothing changes without you reading and merging it**.

There is no automation that opens branches or pull requests, and nothing merges
by itself. Three small workflows exist; you trigger or benefit from each one
knowingly (see Part 6).

Use **Command Prompt** (press the Windows key, type `cmd`, press Enter) unless a
step says PowerShell.

---

## Part 1: Install the prerequisites (one time)

Run these one at a time. Skip anything you already have.

```
winget install --id Git.Git -e
winget install --id GitHub.cli -e
winget install --id Docker.DockerDesktop -e
```

* **Git** stores your code history. **GitHub CLI (`gh`)** lets you talk to GitHub from the prompt.
* **Docker Desktop** runs the whole app (database, message broker, backend, UI). It needs a **restart**, and you must **open Docker Desktop once** and wait until it says "Engine running".

**Close and reopen Command Prompt** after installing so the new programs are found.

Optional, only if you want to run tests on your own PC (GitHub runs them for you anyway).
For the infrastructure code you can also install [OpenTofu](https://opentofu.org/docs/intro/install/) (the open-source Terraform-compatible tool this project uses); GitHub checks it for you either way.

```
winget install --id OpenJS.NodeJS.LTS -e
winget install --id EclipseAdoptium.Temurin.17.JDK -e
```

Check the essentials work:

```
git --version
gh --version
docker --version
```

Also create a free GitHub account at https://github.com if you don't have one.

---

## Part 2: Get the project onto your PC

Pick any folder. Example:

```
mkdir C:\projects
cd C:\projects
tar -xf C:\Users\YOU\Downloads\cardflow-data-platform.zip
cd cardflow
```

(Change the path to where the ZIP really is. Windows 11 has `tar` built in.)

### Put your GitHub username into the project

The project files contain the placeholder word `sm2774us`. Replace it with your
username (example: `sm2774us`). Run this in the same Command Prompt window:

```
powershell -ExecutionPolicy Bypass -File .\scripts\set-owner.ps1 -Owner sm2774us
```

It lists the files it changed (README badge, `CODEsm2774usS`, Kubernetes image
names). It only replaces the whole word, so the word `CODEsm2774usS` stays intact.

---

## Part 3: Run it locally (see it work before publishing)

Make sure Docker Desktop is running, then:

```
docker compose up --build
```

The first run downloads images and takes several minutes. When the logs settle:

| What | Address |
|---|---|
| Web console | http://localhost:8081 |
| API + Swagger docs | http://localhost:8080/swagger-ui.html |
| RabbitMQ admin (user `guest`, password `guest`) | http://localhost:15672 |

In the console open **Live** and click **Simulate traffic**. Stop everything with
`Ctrl+C`, then clean up with:

```
docker compose down -v
```

---

## Part 4: Publish to GitHub (one time)

**1. Turn the folder into a git repo** (the `-M main` matters: it names the branch `main`):

```
git init
git branch -M main
git add -A
git commit -m "initial commit"
```

If git says it doesn't know who you are, run these once and commit again:

```
git config --global user.name "Your Name"
git config --global user.email "you@example.com"
```

**2. Log in to GitHub** (opens your browser, one time only):

```
gh auth login
```

**3. Create the repository and upload:**

```
gh repo create cardflow-data-platform --public --source=. --remote=origin --push
```

Use `--private` instead of `--public` if you prefer. (Note: the branch-protection
ruleset in Part 5 is free on public repos; private repos need a paid GitHub plan.)

**4. Keep branch clutter away automatically.** Merged branches get deleted, and
only squash-merging is allowed, so history stays one tidy commit per change:

```
gh repo edit --delete-branch-on-merge --enable-squash-merge --enable-merge-commit=false --enable-rebase-merge=false
```

**5. Turn on GitHub's built-in security alerts** (alerts only, no automatic PRs):
repo page → **Settings** → **Code security** → enable **Dependabot alerts**.
Leave "Dependabot security updates" and "version updates" **off** if you don't want bot pull requests.

---

## Part 5: Protect `main` (one time, after CI is green)

### 5a. Wait for the first CI run and get it green

Pushing in Part 4 started the `ci` workflow. Watch it:

```
gh run watch
```

It runs three checks named **backend**, **frontend** and **infra**.

> **Honest heads-up:** the backend (Java) code was written without being compiled
> by its author, so the very first backend run may fail on a small mistake. This
> is normal. See exactly what failed:
> ```
> gh run view --log-failed
> ```
> Fix the file, then `git add -A`, `git commit -m "fix: ..."`, `git push`.
> Repeat until all three are green. **Do this before 5b**, because once `main`
> is protected, every fix has to go through a pull request.

### 5b. Turn protection on

Everything below is a one-line command using the file already in the repo
(`.github/rulesets/protect-main.json`):

```
gh api -X POST repos/:owner/:repo/rulesets --input .github/rulesets/protect-main.json
```

`:owner` and `:repo` are filled in automatically from the current folder.

What the rule enforces on `main`:

* nobody can push directly (changes must arrive through a pull request),
* nobody can force-push or delete the branch,
* **backend, frontend and infra must be green** before the merge button works,
* review conversations must be resolved,
* **no bypass, including you**.

**Prefer clicking?** Repo → **Settings** → **Rules** → **Rulesets** → **New ruleset** → **Import a ruleset** → choose `.github/rulesets/protect-main.json` → **Create**.

> **Why 0 required approvals?** GitHub does not let you approve your own pull
> request, so on a one-person repo "1 approval" would lock you out. Your
> supervision is the pull request itself: you open it, read the diff, wait for
> green checks, and click merge yourself. If a teammate joins later, change
> `required_approving_review_count` to `1` in the ruleset.

---

## Part 6: Everyday workflow (keeps branches and PRs to a minimum)

Rule of thumb: **one branch, one pull request, merged by you, at a time.**

```
git switch main
git pull
git switch -c my-change
```

Edit files, then:

```
git add -A
git commit -m "feat: describe your change"
git push -u origin my-change
gh pr create --fill
```

Wait for the checks (`gh pr checks --watch`), **read the changes**
(`gh pr view --web` shows the diff in your browser), then merge:

```
gh pr merge --squash --delete-branch
git switch main
git pull
```

The branch is deleted on GitHub and on your PC. Nothing is left behind.

### What the three workflows do

| Workflow | When it runs | What it does |
|---|---|---|
| `ci` | Automatically, on pushes to `main` and on pull requests | Builds and tests backend and frontend, validates the OpenTofu and Kubernetes files. Never merges or creates anything. |
| `api-tests` | Only when you run `gh workflow run api-tests.yml` | Starts the full app in Docker on GitHub and runs the Karate and Newman API tests. |
| `release` | Only when you push a version tag | Publishes Docker images to GitHub Packages and creates a GitHub Release. |

To make a release, once your PR is merged and `main` is green:

```
git switch main
git pull
git tag v1.0.0
git push origin v1.0.0
```

---

## Part 7: Optional local checks

Only if you installed Node / the JDK in Part 1:

```
cd frontend
npm ci
npm run lint
npm test
cd ..
cd backend
mvn -B test
```

(Maven is also required for the last command: download it from https://maven.apache.org, or skip this and let GitHub run it.)

---

## Troubleshooting

| Problem | Fix |
|---|---|
| `'git' is not recognized` | Close and reopen Command Prompt after installing. |
| Docker errors like "cannot connect to the daemon" | Open Docker Desktop and wait for "Engine running". |
| Port already in use (8080, 8081, 5432, 5672) | Close the other program using it, or run `docker compose down -v` first. |
| `gh api ... rulesets` says 403 or "upgrade" | Private repos need a paid plan for rulesets. Make the repo public or use classic branch protection: Settings → Branches → Add rule → require a pull request and the status checks `backend`, `frontend`, `infra`. |
| CI failed but the log is long | Run `gh run view --log-failed`, or open the run on GitHub and download the **backend-test-reports** artifact (full test failure text). |
| Merge button is greyed out | A check is still running or failed. `gh pr checks` shows which. |
| Rule says a check "is expected" but never appears | The check names must match exactly: `backend`, `frontend`, `infra` (they are the job names in `.github/workflows/ci.yml`). |
| Pushed straight to `main` and it was rejected | That is the protection working. Create a branch (Part 6). |

---

## Quick reference: whole sequence

```
:: one time
winget install --id Git.Git -e
winget install --id GitHub.cli -e
winget install --id Docker.DockerDesktop -e
cd C:\projects
tar -xf C:\Users\YOU\Downloads\cardflow-data-platform.zip
cd cardflow
powershell -ExecutionPolicy Bypass -File .\scripts\set-owner.ps1 -Owner YOUR_GITHUB_USERNAME
git init
git branch -M main
git add -A
git commit -m "initial commit"
gh auth login
gh repo create cardflow-data-platform --public --source=. --remote=origin --push
gh repo edit --delete-branch-on-merge --enable-squash-merge --enable-merge-commit=false --enable-rebase-merge=false
gh run watch
:: when CI is green:
gh api -X POST repos/:owner/:repo/rulesets --input .github/rulesets/protect-main.json

:: every change
git switch -c my-change
git add -A
git commit -m "feat: ..."
git push -u origin my-change
gh pr create --fill
gh pr checks --watch
gh pr view --web
gh pr merge --squash --delete-branch
git switch main
git pull
```
