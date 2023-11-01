# Changelog


## [3.0.0](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/compare/v2.0.0...v3.0.0) (2023-11-01)


### ⚠ BREAKING CHANGES

* update to Hibernate 6.x ([#687](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/687))

### Features

* update to Hibernate 6.x ([#687](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/687)) ([e2882fc](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/e2882fcf5a367355c4040a9526288254ed0c624d))

## [2.0.0](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/compare/v1.9.1...v2.0.0) (2023-10-25)


### ⚠ BREAKING CHANGES

* add support for bit-reversed sequences (server-side) ([#718](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/718))

### Features

* add support for bit-reversed sequences (server-side) ([#718](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/718)) ([efa9a2b](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/efa9a2be3f43702ebb4ebc8ff02de6d1bf315c6e))

## [1.9.1](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/compare/v1.9.0...v1.9.1) (2023-10-25)


### Dependencies

* bump com.google.cloud:google-cloud-spanner-jdbc ([#772](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/772)) ([8174281](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/8174281acba5acb148102e2ae077c4eb7e7f4749))
* bump org.apache.maven.plugins:maven-checkstyle-plugin ([#790](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/790)) ([513ce65](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/513ce656ab5d011947f1d4ca43a9d97e45fb378a))
* bump org.apache.maven.plugins:maven-failsafe-plugin ([#780](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/780)) ([e25c297](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/e25c2973ad190d06fc3476181716931b897b7372))
* bump org.apache.maven.plugins:maven-surefire-plugin ([#779](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/779)) ([1c091a8](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/1c091a8e04b7cc3db1ebd2acc9aeed6889d75f44))

## [1.9.0](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/compare/v1.8.3...v1.9.0) (2023-10-09)


### Features

* support ColumnDefault annotation ([#763](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/763)) ([a7e74de](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/a7e74dec901705cd3efe3462e0a93b6c6a30fe98))


### Documentation

* add locking limitation ([#765](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/765)) ([a33308f](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/a33308ff9d891bbac881de0e686a4494a3dfb59b))
* update limitation section on mutations ([#762](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/762)) ([28c6392](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/28c63921b29f38fc75f10409a357ac38b9f59734))

## [1.8.3](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/compare/v1.8.2...v1.8.3) (2023-10-09)


### Dependencies

* bump com.google.cloud:google-cloud-spanner-jdbc ([#750](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/750)) ([9341d25](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/9341d251737142f20e0da21199c135b11ed1539b))
* bump com.google.cloud:google-cloud-spanner-jdbc ([#756](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/756)) ([729b67c](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/729b67c3b90992478a8dfcb152967f4a3ee96950))

## [1.8.2](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/compare/v1.8.1...v1.8.2) (2023-09-22)


### Bug Fixes

* JDBC connection was leaked during schema creation ([#738](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/738)) ([427fe02](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/427fe02162c1f6b439a7b1f1deee8abc01b45384))


### Dependencies

* bump com.google.cloud:google-cloud-spanner-jdbc ([#744](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/744)) ([ce0ac0c](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/ce0ac0cce9c9211b2ff8acbe32731960c109dfe5))

## [1.8.1](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/compare/v1.8.0...v1.8.1) (2023-09-18)


### Dependencies

* bump com.google.cloud:google-cloud-spanner-jdbc ([#689](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/689)) ([42f590c](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/42f590c8c479111a67d39128d398aae3a951b01b))
* bump com.google.cloud:google-cloud-spanner-jdbc ([#710](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/710)) ([590d17a](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/590d17a6f6896cb0da4806ef13482a86964c9ca7))
* bump com.google.cloud:google-cloud-spanner-jdbc ([#713](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/713)) ([d82079e](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/d82079e8423462d89033fd9cb6a3de2b6315d91a))
* bump com.google.cloud:google-cloud-spanner-jdbc ([#717](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/717)) ([64da3bd](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/64da3bdec5092d1f10ecab376c2b735a4c2e1cfb))
* bump com.google.cloud:google-cloud-spanner-jdbc ([#725](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/725)) ([1859fd9](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/1859fd94e81b71902ca9444832dcaacfc0684f34))
* bump com.google.cloud:google-cloud-spanner-jdbc ([#729](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/729)) ([fe52341](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/fe52341093a1470a60ce1c16eb02d6faa72e38e0))
* bump com.google.cloud:google-cloud-spanner-jdbc from 2.11.4 to 2.11.5 in /google-cloud-spanner-hibernate-dialect ([#680](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/680)) ([e388eaf](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/e388eaf6cc65f5519b9c70f79eb1d719838f66f1))
* bump org.apache.commons:commons-lang3 from 3.12.0 to 3.13.0 in /google-cloud-spanner-hibernate-dialect ([#681](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/681)) ([e7b20b0](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/e7b20b06522bf3e812b168fa5c871386db10498f))
* bump org.apache.maven.plugins:maven-javadoc-plugin ([#728](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/728)) ([cf9c1eb](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/cf9c1ebd9233cc5a21997af45de8220f28fd1ebd))

## [1.8.0](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/compare/v1.7.0...v1.8.0) (2023-07-24)


### Features

* foreign key on delete cascade. ([#596](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/596)) ([bcd9416](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/bcd94161a7b2934c3a226a2160f2502efbaab934))


### Bug Fixes

* add default configuration for license header lint. ([#675](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/675)) ([131098c](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/131098ca57b4b676e7079c18d0b2c43f35c93018))


### Documentation

* bump versions in README ([#591](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/591)) ([f835282](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/f835282b607e898a0847f910aa089e6dd8995afe))


### Dependencies

* bump google-cloud-spanner-jdbc from 2.10.0 to 2.11.0 in /google-cloud-spanner-hibernate-dialect ([#659](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/659)) ([2891c46](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/2891c46b50cad8a1d2fce4aae08e420de6f1bfca))
* bump google-cloud-spanner-jdbc from 2.11.0 to 2.11.2 in /google-cloud-spanner-hibernate-dialect ([#664](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/664)) ([928b3cf](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/928b3cf20d3107bb129b401edd8f609756c708a8))
* bump google-cloud-spanner-jdbc from 2.11.2 to 2.11.4 in /google-cloud-spanner-hibernate-dialect ([#670](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/670)) ([2f48c0f](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/2f48c0f2871b0cfdaf795392f7d4200d0d237b61))
* bump google-cloud-spanner-jdbc from 2.9.12 to 2.9.14 in /google-cloud-spanner-hibernate-dialect ([#623](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/623)) ([5902961](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/5902961a26a43715273baab1e6ebe1b46c8f8289))
* bump google-cloud-spanner-jdbc from 2.9.14 to 2.10.0 in /google-cloud-spanner-hibernate-dialect ([#650](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/650)) ([078a270](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/078a27077f5754f336b52cbef32d0ba742fe5c0b))
* bump google-cloud-spanner-jdbc from 2.9.9 to 2.9.12 in /google-cloud-spanner-hibernate-dialect ([#614](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/614)) ([9c8eb4a](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/9c8eb4ab79bef005429b4662a2a6d8cfc263c689))
* bump maven-checkstyle-plugin from 3.2.1 to 3.2.2 in /google-cloud-spanner-hibernate-dialect ([#611](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/611)) ([af8fa2f](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/af8fa2f49edf268e720a5a37bafd2a86c44420e8))
* bump maven-checkstyle-plugin from 3.2.2 to 3.3.0 in /google-cloud-spanner-hibernate-dialect ([#645](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/645)) ([a1e33ad](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/a1e33ad0f539ab23b6ec97848d6c2aed55f4a7b6))
* bump maven-deploy-plugin ([#599](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/599)) ([66f41c3](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/66f41c322e636acaa0725e3c513002cda0fc0055))
* bump maven-failsafe-plugin from 3.0.0 to 3.1.0 in /google-cloud-spanner-hibernate-dialect ([#630](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/630)) ([b57a7cd](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/b57a7cdac4495358d798843dfd992dcaa9a97d06))
* bump maven-gpg-plugin from 3.0.1 to 3.1.0 in /google-cloud-spanner-hibernate-dialect ([#627](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/627)) ([d6dfb9b](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/d6dfb9b88615e4aef0b40cfaa1e384847c622797))
* bump maven-source-plugin from 3.2.1 to 3.3.0 in /google-cloud-spanner-hibernate-dialect ([#640](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/640)) ([69d1171](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/69d1171b56733b078393a0ea0061577cb927062a))
* bump maven-surefire-plugin from 3.0.0 to 3.1.0 in /google-cloud-spanner-hibernate-dialect ([#631](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/631)) ([a352427](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/a35242786b69da6cedf66fc1830fae9365cc13c8))
* bump maven-surefire-plugin from 3.1.0 to 3.1.2 in /google-cloud-spanner-hibernate-dialect ([#653](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues/653)) ([0b42277](https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/commit/0b42277b8319ef0f9b31a624fa1fdf0b4d399136))

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
