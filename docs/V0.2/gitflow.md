# Git flow

The bootstrap script initializes `main`, `testing` and `develop`, then checks out `develop` as the working branch. The intended flow is:

1. start feature branches from `develop`;
2. open pull requests into `develop`;
3. promote stabilized changes to `testing`;
4. promote releases to `main`.

Useful commands:

```bash
make feature-start name=my-feature
make feature-push
make pr
```

When GitHub CLI is authenticated, `init-project.sh` can also create or reuse the remote repository, set topics, configure the default branch and apply branch protection rules.
