FROM liquibase/liquibase:latest

USER root

ADD https://storage.googleapis.com/simba-bq-release/jdbc/SimbaBigQueryJDBC42-1.3.2.1003.zip /liquibase/SimbaBigQueryJDBC42-1.3.2.1003.zip

RUN unzip /liquibase/SimbaBigQueryJDBC42-1.3.2.1003.zip GoogleBigQueryJDBC42.jar -d /liquibase/lib/

USER liquibase