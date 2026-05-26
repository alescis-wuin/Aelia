# Git synchronization

From an existing clone of the private repository:

```bash
git checkout develop
cp -R /path/to/Aelia/. .
git status
mvn verify
git add .
git commit -m "Reproduce weather dashboard mockup"
git push origin develop
```

The repository URL supplied as `Aeliea` resolves to the existing `Aelia` repository. Maven metadata and source packages use `Aelia` and `fr.alescis.aelia`.
