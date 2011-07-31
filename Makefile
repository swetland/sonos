
SRCS := $(shell find net -name \*.java)

all: sonos.jar

sonos.jar: $(SRCS)
	rm -rf out
	mkdir -p out
	javac -d out $(SRCS)
	rm -rf sonos.jar
	jar cfe sonos.jar net.frotz.sonos.app -C out/ .

clean::
	rm -rf out sonos.jar
