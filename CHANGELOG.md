# Changelog

## [0.1.0](https://github.com/only-climb/only-climb-api/compare/v0.0.1...v0.1.0) (2026-06-20)


### Features

* add complete CI/CD pipeline with Dependabot auto-merge ([d13707d](https://github.com/only-climb/only-climb-api/commit/d13707dfbe1fbcceb938c4c53d771d37938c1ecd))
* add complete CI/CD pipeline with Dependabot auto-merge ([0540eb0](https://github.com/only-climb/only-climb-api/commit/0540eb08205564592edc27f80d7005ccd6f5bdcd))
* **assessment:** vertical slice for user assessment results ([906b4cc](https://github.com/only-climb/only-climb-api/commit/906b4ccb2563a99f3848c22664e5362ff183fcee))
* **auth:** integrate Clerk via OAuth2 JWT + Svix webhook ([56e22b6](https://github.com/only-climb/only-climb-api/commit/56e22b62f4b5520931a8437d6dba671892fe48df))
* **catalogs:** public read-only reference catalog endpoints + seeds ([dc31c10](https://github.com/only-climb/only-climb-api/commit/dc31c1054dfb9e4c11f08834db98a67083813e43))
* **db:** initial schema with hexagonal-friendly payments and AI job tracking ([86f66c1](https://github.com/only-climb/only-climb-api/commit/86f66c143e902358c0847a690869f30145a7bf19))
* **docker:** add Dockerfile for building and running the application ([4ebab4e](https://github.com/only-climb/only-climb-api/commit/4ebab4eaa543338de9da69ac5ef3cdd32c201bf8))
* **docs:** update README with CI/CD pipeline, Docker instructions, and subscription details ([7aa5a26](https://github.com/only-climb/only-climb-api/commit/7aa5a26e79d0527df7a2b92ed60c8b8671c8719e))
* **exercise:** vertical slice for platform & user-created exercises ([598d5a5](https://github.com/only-climb/only-climb-api/commit/598d5a5ab0f34c754b0304a540d0a88fb3fd20b0))
* **follow:** user follow graph endpoints with idempotent edges ([ac51cd7](https://github.com/only-climb/only-climb-api/commit/ac51cd7a7dd9d9573d152be23bc0a85f40eebef4))
* **goal:** vertical slice for user training goals ([2b7a88b](https://github.com/only-climb/only-climb-api/commit/2b7a88ba3e9874b7ca5d7c26ef32c478476626ac))
* **readme:** add comprehensive documentation for Only Climb API ([07a3634](https://github.com/only-climb/only-climb-api/commit/07a36342579fd753f23257030157104bc8590c04))
* **release:** add GitHub Actions workflow for automated build, test, and release process ([f24d549](https://github.com/only-climb/only-climb-api/commit/f24d549902d13a710c6c1371f18f588364542fe1))
* **release:** update release-please-action to v5 and add release manifest ([3ad9f1d](https://github.com/only-climb/only-climb-api/commit/3ad9f1d5e9e481eb33d46a46bde95c8e993cdcc7))
* **subscription:** Implement JPA repositories and adapters for subscription management ([eb8dbb8](https://github.com/only-climb/only-climb-api/commit/eb8dbb8c29adf59d0643b0ef356940b23ce13b05))
* **tests:** mock ProvisionFreeSubscriptionUseCase in UserServiceTest ([fc3f9ea](https://github.com/only-climb/only-climb-api/commit/fc3f9ea3a812452e7825897b7dca6ad22795d1e1))
* **training-plan:** vertical slice for multi-week training plans ([9404921](https://github.com/only-climb/only-climb-api/commit/94049211caca6af1874281bc718c8f6daebc20d2))
* **user:** vertical slice for User and UserProfile ([8358fb1](https://github.com/only-climb/only-climb-api/commit/8358fb19ffc8e657875ff594ef3c1df5e7b2a5e9))
* **webhook:** initialize ObjectMapper in StripeWebhookController ([fc3f9ea](https://github.com/only-climb/only-climb-api/commit/fc3f9ea3a812452e7825897b7dca6ad22795d1e1))
* **workflows:** add GitHub Actions workflow for build, test, and package processes ([fc3f9ea](https://github.com/only-climb/only-climb-api/commit/fc3f9ea3a812452e7825897b7dca6ad22795d1e1))
* **workflows:** add GitHub Actions workflows for artifact attachment and release management ([1d5ee58](https://github.com/only-climb/only-climb-api/commit/1d5ee58f875d711b257cd56e13c4381c56e3ab4b))
* **workflows:** enhance workflow documentation and improve step naming for clarity ([1532c1b](https://github.com/only-climb/only-climb-api/commit/1532c1bddd53b6126391582ca5d922a18fd4daa3))
* **workflows:** remove deprecated release-please workflow and update actions to v5 in verify workflow ([39c6b1d](https://github.com/only-climb/only-climb-api/commit/39c6b1da75b007f09974e3882a7feef9f10e71ff))
* **workflows:** remove release-please workflow ([65a698d](https://github.com/only-climb/only-climb-api/commit/65a698d56584b790b0a77bec1e1fa4ea2eadae17))
* **workflows:** update actions to latest versions in workflow files ([733e23a](https://github.com/only-climb/only-climb-api/commit/733e23ae29b9c4feb5336a5e98af8d3aa4e458d1))
* **workflows:** update actions to latest versions in workflow files ([7ee5bb6](https://github.com/only-climb/only-climb-api/commit/7ee5bb6b29c3f9c1c87e8e98758d50f3d0b58537))
* **workout-log:** vertical slice for personal workout session logs ([2401ef2](https://github.com/only-climb/only-climb-api/commit/2401ef2b0c4b602c42d485d379424b75faf039d8))
* **workout-template:** vertical slice for platform & user-created templates with fork ([c171f26](https://github.com/only-climb/only-climb-api/commit/c171f26938d4b50d125e8424581bfe86f150dc70))


### Bug Fixes

* **deps:** bump org.springdoc:springdoc-openapi-starter-webmvc-ui ([b474e23](https://github.com/only-climb/only-climb-api/commit/b474e23f56ad59a9319aec6c2af022123d591dc1))
* **deps:** bump org.springdoc:springdoc-openapi-starter-webmvc-ui from 2.8.13 to 3.0.3 ([2eb42c0](https://github.com/only-climb/only-climb-api/commit/2eb42c0215896cb907c5382edf4c1710b6158515))
* **deps:** bump org.springframework.boot:spring-boot-starter-parent ([#7](https://github.com/only-climb/only-climb-api/issues/7)) ([b18e158](https://github.com/only-climb/only-climb-api/commit/b18e158ba46ae005c6d9948e6a64f7c7c070ff11))
* **i18n:** make Spanish the default project locale ([d99468d](https://github.com/only-climb/only-climb-api/commit/d99468deda2d70f4794da71391d9e68ffaf133e2))


### Documentation

* lock UUID+Long and i18n translation table patterns in instructions ([f2d7cd0](https://github.com/only-climb/only-climb-api/commit/f2d7cd0d98f5ce1421a4437b341a93797ceb3408))
* rewrite business rules with climbing-specific domain model ([6ef48ff](https://github.com/only-climb/only-climb-api/commit/6ef48ff84b0bbfe505b7b33f220c26ce3a878980))
