.PHONY: run test verify clean check import-fonts

run:
	mvn javafx:run

test:
	mvn test

verify:
	mvn verify

clean:
	mvn clean

check:
	./scripts/check-environment.sh

import-fonts:
	./scripts/import-local-fonts.sh "$(LUCIOLE_ZIP)" "$(HACK_ZIP)"
