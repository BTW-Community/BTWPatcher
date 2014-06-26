MCVER ?= 1.7.10
MCPROFILE ?= MCPatcher
MCDIR ?= $(HOME)/.minecraft
JAVA_OPTS ?= -Xmx256M
EXT_OPTS ?=

VERSIONS_DIR = $(MCDIR)/versions
VERSIONS_URL = https://s3.amazonaws.com/Minecraft.Download/versions/versions.json
VERSIONS_LCL = src/resources/versions.json
MCJAR = $(VERSIONS_DIR)/$(MCVER)/$(MCVER).jar

MCPATCHER = out/artifacts/mcpatcher/mcpatcher.jar

JIP = $(HOME)/jip-1.2/profile/profile.jar
LAUNCH4J = $(HOME)/launch4j/launch4j
LAUNCH4J_XML = launch4j.xml

DOC_OUT = doc/javadoc
DOC_SRC = com.prupe.mcpatcher
DOC_SRCPATH = shared/src:stubs/src:newcode/src:src:
DOC_CLASSPATH = $(shell ls -1 lib/*.jar | egrep -v "javadoc|sources|natives" | tr '\n' :)

TEST_OPTS = -ignoresavedmods -ignorecustommods -enableallmods -auto -loglevel 5 -profile "$(MCPROFILE)" -mcversion "$(MCVER)"
JAVA_CMD = java $(JAVA_OPTS) -jar $(MCPATCHER) $(EXT_OPTS)
TEST_CMD = $(JAVA_CMD) $(TEST_OPTS)
TEST_LOG = test.log
GOOD_LOG = good.log
TMPDIR = t.1
FILTER = ./testfilter.pl

.PHONY: default build release run test testfilter control testclean clean
.PHONY: rmall javadoc profile updversions

default:

build:
	ant

release: $(MCPATCHER)
	cp -pf $(MCPATCHER) mcpatcher-$(shell java -jar $(MCPATCHER) -version).jar
	sed -e 's/VERSION/$(shell java -jar $(MCPATCHER) -version)/g' $(LAUNCH4J_XML) > $(LAUNCH4J_XML).tmp
	$(LAUNCH4J) $(shell pwd)/$(LAUNCH4J_XML).tmp
	rm -f $(LAUNCH4J_XML).tmp

run: $(MCPATCHER)
	$(JAVA_CMD)

test: $(MCPATCHER)
	time $(TEST_CMD) > $(TEST_LOG) 2>&1
	diff -c $(GOOD_LOG) $(TEST_LOG)
	rm -f $(TEST_LOG)

testfilter: $(MCPATCHER)
	time $(TEST_CMD) > $(TEST_LOG) 2>&1
	@$(FILTER) $(TEST_LOG) > $(TEST_LOG).1
	@$(FILTER) $(GOOD_LOG) > $(GOOD_LOG).1
	diff -c $(GOOD_LOG).1 $(TEST_LOG).1
	rm -f $(TEST_LOG) $(TEST_LOG).1 $(GOOD_LOG).1

control: $(TEST_LOG)
	cp -f $(TEST_LOG) $(GOOD_LOG)

testclean:
	rm -f $(TEST_LOG) $(TEST_LOG).1 $(GOOD_LOG).1

clean: testclean
	rm -rf $(MCPATCHER) $(DOC_OUT) out $(LAUNCH4J_XML).tmp mcpatcher-*.jar mcpatcher-*.exe profile.txt profile.xml

rmall:
	rm -rf $(VERSIONS_DIR)/1*-*

javadoc:
	rm -rf $(DOC_OUT)
	mkdir -p $(DOC_OUT)
	javadoc -protected -splitindex -classpath $(DOC_CLASSPATH) -d $(DOC_OUT) $(DOC_SRC) -sourcepath $(DOC_SRCPATH)

profile: $(MCPATCHER) $(JIP)
	java -Xmx512M -javaagent:$(JIP) -Dprofile.properties=profile.properties -jar $(MCPATCHER) $(TEST_OPTS) > $(TEST_LOG) 2>&1

updversions:
	wget -O $(VERSIONS_LCL) $(VERSIONS_URL)
	dos2unix $(VERSIONS_LCL)
