# Changelog


## [1.7.0](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/compare/v1.6.0...v1.7.0) (2023-03-21)


### Features

* exclude ranges option for bit-reversed sequence ([#585](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/585)) ([c7925e9](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/c7925e9c78ef5ef648016f75b06ca3b1fcf0cab2))


### Dependencies

* Bump google-cloud-spanner-jdbc ([#576](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/576)) ([873cc70](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/873cc70f9d389f65bcb40209eea0780aa6ce64a8))
* bump google-cloud-spanner-jdbc ([#588](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/588)) ([7f8e445](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/7f8e445f9ab84cd069b10252c178a302a56c319a))
* bump google-cloud-spanner-jdbc from 2.9.5 to 2.9.7 in /google-cloud-spanner-hibernate-dialect ([#564](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/564)) ([c60ad6f](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/c60ad6ff904324e16ddb7f6aaf8d27c2919b6073))
* bump hibernate.version from 5.6.10.Final to 5.6.15.Final in /google-cloud-spanner-hibernate-dialect ([#542](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/542)) ([b2b1206](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/b2b1206dd49a517712562062a242c22ca38ac15a))
* bump maven-checkstyle-plugin from 3.1.2 to 3.2.1 in /google-cloud-spanner-hibernate-dialect ([#517](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/517)) ([f736f92](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/f736f92a5ff04f0f9b5fd16e9821277f6fc606e8))
* bump maven-deploy-plugin ([#535](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/535)) ([deaf26a](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/deaf26a50aea0c20638ac73892732170eb2fb31d))
* bump maven-failsafe-plugin ([#581](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/581)) ([20e7b77](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/20e7b771f402c6783cb4a86baf3c7212de33bf59))
* bump maven-failsafe-plugin from 3.0.0-M8 to 3.0.0-M9 in /google-cloud-spanner-hibernate-dialect ([#548](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/548)) ([09e8e22](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/09e8e22259744d63ad9c4f7b222a41afa22ab91a))
* bump maven-jar-plugin from 3.2.2 to 3.3.0 in /google-cloud-spanner-hibernate-dialect ([#523](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/523)) ([cff1d66](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/cff1d6657b6898ec835fb55b2aacb61dd326da58))
* bump maven-javadoc-plugin from 3.4.1 to 3.5.0 in /google-cloud-spanner-hibernate-dialect ([#554](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/554)) ([c56ef22](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/c56ef2279a2e11e9401d93b7fa446736c3077678))
* bump maven-surefire-plugin from 3.0.0-M7 to 3.0.0-M8 in /google-cloud-spanner-hibernate-dialect ([#519](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/519)) ([90f63f7](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/90f63f7f71fbf27c54a37568f4aac652ef8d5999))
* bump maven-surefire-plugin from 3.0.0-M8 to 3.0.0-M9 in /google-cloud-spanner-hibernate-dialect ([#547](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/547)) ([5086bc2](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/5086bc2147cce4291e8615fe2b2a0077d14c7f8a))

## [1.6.0](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/compare/v1.5.6...v1.6.0) (2023-01-19)


### Features

* support bit-reversed sequences ([#478](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/478)) ([ab80b04](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/ab80b04ab86197fc1fed14e49daec5d4b3d51e66))


### Bug Fixes

* retry transaction for sequence.next_val update ([#486](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/486)) ([132bb3c](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/132bb3c081a57dca96d6db47c25d5ec3fcec287e))
* use separate JDBC connection for extraction ([#481](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/481)) ([cf5f96d](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/cf5f96d2f8e8d33704816a510ec3fee51b43950d))

## [1.5.6](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/compare/v1.5.4...v1.5.6) (2022-10-26)


### Bug Fixes

* do not ignore exception during schema migration ([#471](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/471)) ([71e53f3](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/71e53f379287a39863527674f8ddfc91b93ffcb1))

### Dependencies

* bump google-cloud-spanner-jdbc from 2.7.3 to 2.7.4 ([#449](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/449)) ([c774d71](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/c774d71c29471ce89729046e22e93f4f4f0cbe00))
* bump hibernate.version from 5.6.9.Final to 5.6.10.Final ([#456](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/456)) ([8b83632](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/8b836326300032c21b69cdc07275788b1302a422))


## [1.5.5](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/compare/v1.5.4...v1.5.5) (2022-07-18)

The 1.5.5 release never went out, and 1.5.4 will be succeeded by 1.5.6.

## [1.5.4](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/compare/1.5.3...v1.5.4) (2022-06-27)


### Dependencies

* Bump google-cloud-spanner-jdbc to 2.7.3 ([4ad5262](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/4ad52625bdf5425ea98f8c2fa1da4a1fd897fd4d))
* Bump hibernate.version to 5.6.9.Final ([4ad5262](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/4ad52625bdf5425ea98f8c2fa1da4a1fd897fd4d))


## [1.5.3](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/compare/1.5.2...1.5.3) (2022-03-01)
Upgrades the project to Hibernate 5.6.

## [1.5.2](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/compare/1.5.1...1.5.2) (2021-12-10)
This release upgraded dependencies including log4j.

## [1.5.1](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/compare/1.5.0...1.5.1) (2021-10-04)
This release introduces the com.google.cloud.spanner.hibernate.type.SpannerJsonType which allows for mapping JSON columns to Hibernate entity fields. The Custom Spanner Types section explains how to use this feature.


## [1.5.0](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/compare/1.4.0...1.5.0) (2021-03-29)
This release adds support to map Spanner Array column types to List<?> fields in Hibernate entities.

Please see the Array Column documentation or example code for how to use this feature.

In addition, this release adds several dependency version upgrades and minor bug fixes.


## [1.4.0](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/compare/1.3.0...1.4.0) (2020-09-29)
This release adds important bug fixes to improve the Cloud Spanner Hibernate Dialect support.

You can now use the Spanner Dialect with other dialects without having the Spanner dialect interfere with the Hibernate settings for other databases. Previously you would need a separate Maven profile to be able to connect to both Spanner and another database through Hibernate. (Fixed by #208 thanks to olavloite@)

Statements using both limits and offsets will now be generated correctly. (#207)



## [1.3.0](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/compare/1.2.0...1.3.0) (2020-09-15)
This release adds support for the NUMERIC data type.

Hibernate entities can now declare java.math.BigDecimal fields and these will be correctly interpreted to use the NUMERIC type in Cloud Spanner in DDL and DML statements.



## [1.2.0](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/compare/1.1.0...1.2.0) (2020-04-23)
This release extends the Cloud Spanner Hibernate dialect to generate Foreign Key statements during schema generation.

Changes since v1.1.0:

When Hibernate is started with hibernate.hbm2ddl.auto enabled, it will generate the correct Foreign Key clauses in the generated DDL statements for the entities using @OneToOne, @OneToMany, and @ManyToOne annotations.


## [1.1.0](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/compare/1.0.0...1.1.0) (2020-02-25)
This release provides additional features for the Google Cloud Spanner Dialect for Hibernate ORM.

Changes since v1.0.0:

Schema generation in Hibernate is functional for all schema-generation modes: CREATE, CREATE-DROP, and UPDATE.
Introduced the @Interleaved annotation for generating Interleaved tables in Spanner through Hibernate.
Added support for using @Column(unique = true) which marks columns as unique in Hibernate entities in Spanner.


## [1.0.0](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/compare/0.1.0...1.0.0) (2019-11-15)
The first GA release of the Google Cloud Spanner Dialect for Hibernate ORM.

Changes since v0.1.0:

InlineIdsOrClauseBulkIdStrategy is now automatically configured as the default bulk id strategy (#151)
Schema generation DDL statements are now batched for schema creation and dropping (#148)


## 0.1.0 (2019-10-15)
* The first beta release of the Google Cloud Spanner Dialect for Hibernate ORM
