keytool -importcert -trustcacerts -file rds-combined-ca-bundle.pem -alias rds -keystore rds-ca-certs -storepass $CA_STORE_PASSWORD -noprompt && java -jar nio.jar
