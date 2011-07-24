
SRCS := $(wildcard net/frotz/sonos/*.java)

all: sonos.jar

sonos.jar: $(SRCS)
	javac $(SRCS)
	rm -rf sonos.jar
	zip -qr sonos.jar META-INF net -x \*.java

clean::
	rm -rf net/frotz/sonos/*.class sonos.jar
