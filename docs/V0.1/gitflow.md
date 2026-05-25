# GitFlow V0.1

## Branches

- `main`: stable release branch.
- `testing`: integration and validation branch.
- `develop`: default branch for daily development.
- `feature/*`: short-lived feature branches opened against `develop`.

## Bootstrap

Run:

```bash
./init-project.sh
```

The script can initialize Git, create the GitHub remote repository, push the first commit, create `testing` and `develop`, set `develop` as default and configure pull-request-only protections.

## Protection model

The generated protection uses:

- required pull-request review;
- stale review dismissal;
- admin enforcement;
- blocked force pushes;
- blocked branch deletion;
- required conversation resolution.

Status checks are not made mandatory during the first bootstrap because the repository has no successful check context before the first GitHub Actions run.
