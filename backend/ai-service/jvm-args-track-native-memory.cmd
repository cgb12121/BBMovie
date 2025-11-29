java -XX:NativeMemoryTracking=detail \
     -XX:+UnlockDiagnosticVMOptions \
     -XX:+PrintNMTStatistics \
     -Xms2G -Xmx4G \
     -jar ai-service.jar