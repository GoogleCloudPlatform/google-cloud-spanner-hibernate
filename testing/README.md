# Spanner-Hibernate Integration Testing

This directory contains a script which allows you to run the Spanner-Hibernate dialect against
the integration tests provided in the [Hibernate ORM repository](https://github.com/hibernate/hibernate-orm).

## Instructions

1. Install Python3 on your machine.

    Verify you have it installed by running:
    
    ```SHELL
    $ python3 --version
   
    Python 3.5.4rc1 
    ```

2. Modify `databases.gradle` to include the correct JDBC URL to connect to your Spanner instance.
You will need to specify the correct GCP project ID, Spanner instance, and table.

    Example: 
    
    ```
    'jdbc.url' : 'jdbc:cloudspanner://;Project=gcp_project_id;Instance=spanner_instance_name;Database=my_db'
    ```

3. Run `python3 run-tests.py`. Note that the script will run all the integration tests in the
directory. It is likely you may want to run a specific test for debugging; this can be done
by modifying the script and filtering the tests using `--tests` in the Gradle command.

    Example: `hibernate-orm/gradlew test -p hibernate-orm/documentation --tests <TEST_NAME>`
