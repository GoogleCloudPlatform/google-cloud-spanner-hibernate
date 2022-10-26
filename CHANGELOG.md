# Changelog


## [1.5.6](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/compare/v1.5.5...v1.5.6) (2022-10-26)


### Bug Fixes

* do not ignore exception during schema migration ([#471](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/471)) ([71e53f3](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/71e53f379287a39863527674f8ddfc91b93ffcb1))

## [1.5.5](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/compare/v1.5.4...v1.5.5) (2022-07-18)


### Dependencies

* bump google-cloud-spanner-jdbc from 2.7.3 to 2.7.4 ([#449](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/449)) ([c774d71](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/c774d71c29471ce89729046e22e93f4f4f0cbe00))
* bump hibernate.version from 5.6.9.Final to 5.6.10.Final ([#456](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/456)) ([8b83632](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/8b836326300032c21b69cdc07275788b1302a422))

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
