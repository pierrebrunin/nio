exec keytool -importcert -trustcacerts -file rds-combined-ca-bundle.pem -alias rds -keystore rds-ca-certs -storepass $STOREPASSWORD -noprompt && java -jar nio.jar
