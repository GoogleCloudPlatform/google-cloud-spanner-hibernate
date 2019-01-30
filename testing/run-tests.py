import os
import subprocess

'''
Script for running Hibernate Integration tests against the
Spanner-Hibernate dialect.
'''

HIBERNATE_TESTS_REPO = 'https://github.com/dzou/hibernate-orm.git'

# Install
subprocess.run('mvn install -DskipTests -f ../pom.xml', shell=True)

# Clone the Hibernate Tests repository if not present.
subdirectories = [folder for folder in os.listdir('.') if os.path.isdir(folder)]
if 'hibernate-orm' in subdirectories:
  print('The hibernate-orm directory already exists; will omit cloning step.')
else:
  subprocess.run(['git', 'clone', HIBERNATE_TESTS_REPO])

# Copy the JDBC driver to directory if it does not already exist.
if 'CloudSpannerJDBC42.jar' not in os.listdir('hibernate-orm/libs'):
  print('Downloading Spanner JDBC driver')
  subprocess.run('gsutil cp gs://spanner-jdbc-bucket/CloudSpannerJDBC42.jar hibernate-orm/libs/', shell=True)

# Update the databases.gradle file with the current version.
subprocess.run('cp databases.gradle hibernate-orm/gradle/databases.gradle', shell=True)

# Run some tests.
# Modify this to filter down to a specific test with --test TEST_NAME
subprocess.run('hibernate-orm/gradlew test -p hibernate-orm/documentation', shell=True)
