src = $(wildcard *.java)
obj = $(src:.java=.class)

jetris.jar : $(obj)
	jar cfe $@ Main *.class

%.class : %.java
	javac $<

.PHONY: clean

clean :
	rm -f *.class *.jar
