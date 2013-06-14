MCVER ?= 13w23b
EXT_OPTS ?=

VERSIONSDIR = ../versions
MCJAR = $(VERSIONSDIR)/$(MCVER)/$(MCVER).jar
PATCHDIR = $(VERSIONSDIR)/$(MCVER)-mcpatcher
MCPROFILE = Minecraft $(MCVER)

MCPATCHER = out/artifacts/mcpatcher/mcpatcher.jar
MODJAR = ../mcpatcher-mods/mcpatcher-builtin.jar

JIP = $(HOME)/jip-1.2/profile/profile.jar
LAUNCH4J = $(HOME)/launch4j/launch4j
LAUNCH4J_XML = launch4j.xml

CLASSPATH = lib/javassist.jar
PACKAGE = com.prupe.mcpatcher
DOC_OUT = doc/javadoc
DOC_SRC = $(PACKAGE)
DOC_SRCPATH = shared/src:stubs/src:newcode/src:src:

TEST_OPTS = -ignoresavedmods -ignorecustommods -enableallmods -auto -loglevel 5 $(EXT_OPTS)
TEST_LOG = test.log
GOOD_LOG = good.log
TMPDIR = t.1
FILTER = ./testfilter.pl

.PHONY: default build release run runexp test testfilter control clean restore
.PHONY: javadoc profile modjar

default:

build:
	@echo "WARNING: ant build does not work, use IntelliJ IDEA"
	@echo "http://www.jetbrains.com/idea/download/"
	ant

release: $(MCPATCHER)
	cp -pf $(MCPATCHER) mcpatcher-$(shell java -jar $(MCPATCHER) -version).jar
	sed -e 's/VERSION/$(shell java -jar $(MCPATCHER) -version)/g' $(LAUNCH4J_XML) > $(LAUNCH4J_XML).tmp
	$(LAUNCH4J) $(shell pwd)/$(LAUNCH4J_XML).tmp
	rm -f $(LAUNCH4J_XML).tmp

run: $(MCPATCHER)
	java -jar $(MCPATCHER) $(EXT_OPTS)

runexp: $(MCPATCHER)
	java -jar $(MCPATCHER) $(EXT_OPTS) -experimental

test: $(MCPATCHER)
	time java -jar $(MCPATCHER) $(TEST_OPTS) -profile "$(MCPROFILE)" > $(TEST_LOG) 2>&1
	diff -c $(GOOD_LOG) $(TEST_LOG)
	rm -f $(TEST_LOG)

testfilter: $(MCPATCHER)
	time java -jar $(MCPATCHER) $(TEST_OPTS) -profile "$(MCPROFILE)" > $(TEST_LOG) 2>&1
	@$(FILTER) $(TEST_LOG) > $(TEST_LOG).1
	@$(FILTER) $(GOOD_LOG) > $(GOOD_LOG).1
	diff -c $(GOOD_LOG).1 $(TEST_LOG).1
	rm -f $(TEST_LOG) $(TEST_LOG).1 $(GOOD_LOG).1

control: $(TEST_LOG)
	cp -f $(TEST_LOG) $(GOOD_LOG)

testclean:
	rm -f $(TEST_LOG) $(TEST_LOG).1 $(GOOD_LOG).1

clean: testclean
	rm -rf $(MCPATCHER) $(DOC_OUT) $(MODJAR) out $(LAUNCH4J_XML).tmp mcpatcher-*.jar mcpatcher-*.exe profile.txt profile.xml

restore:
	rm -rf $(PATCHDIR)

javadoc:
	rm -rf $(DOC_OUT)
	mkdir -p $(DOC_OUT)
	javadoc -protected -splitindex -classpath $(CLASSPATH) -d $(DOC_OUT) $(DOC_SRC) -sourcepath $(DOC_SRCPATH)

profile: $(MCPATCHER) $(JIP)
	java -Xmx512M -javaagent:$(JIP) -Dprofile.properties=profile.properties -jar $(MCPATCHER) $(TEST_OPTS) > $(TEST_LOG) 2>&1

modjar: $(MCPATCHER)
	rm -rf $(TMPDIR)
	mkdir -p $(TMPDIR)
	cd $(TMPDIR) && jar -xf ../$(MCPATCHER)
	cd $(TMPDIR) && rm -rf javassist META-INF *.class com/intellij com/prupe/mcpatcher/*.class
	cd $(TMPDIR) && jar -cf ../$(MODJAR) *
	rm -rf $(TMPDIR)
