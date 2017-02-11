CLASSPATH="./bin:./packages/commons-math3-3.5.jar:./packages/postgresql-9.3-1102.jdbc4.jar:./packages/LibSVM-1.0.6.jar:./packages/weka.jar:./packages/json-20140107.jar:./packages/pdm-timeseriesforecasting-ce-stable.jar"

java -Xmx1024M -cp $CLASSPATH execute.RunDemoSocket 8889 backEndConfigEllipse.json  
#java -Xmx4196M -cp $CLASSPATH execute.RunDemoSocket 8889 backEndConfigEllipse.json  
#java -Xmx1024M -cp $CLASSPATH execute.RunDemo 8889 frontEndConfig.json backEndConfig.json  
