cp=bin
for jar in lib/*.jar
do
  cp="$cp;$jar"
done

java -classpath "$cp" org.junit.runner.JUnitCore $1

