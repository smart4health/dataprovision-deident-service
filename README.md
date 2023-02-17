# Smart4Health Data-provision De-Identification Service

## Acknowledgements

<img src="./img/eu.jpg" align="left" alt="European Flag" width="60">

This project has received funding from the European Union’s Horizon 2020 research and innovation programme under grant agreement No 826117.

## About

The De-Identification Service takes a FHIR bundle that has been provided by a citizen via
the [MyScience App](https://github.com/smart4health/my-science-app), strips all identifying information and uploads it
to the Smart4Health Research Platform.

It also takes care of harmonizing FHIR Coding elements to standardized ones in the OMOP Common Data Model. For this a
query service QOMOP was implemented, see
here: [dataprovision-qomop-service](https://github.com/smart4health/dataprovision-qomop-service).

## Basic flow

- Receive Bundle of FHIR from a user ([downloader](downloader))
- Submit to queue managed by events in order to process the FHIR resources in this order:
    - TypeFilter ([type-filter](type-filter)): Reject resources that are not used by the Research Platform
    - Patient Deduplicator ([patient-deduplicator](patient-deduplicator)): Ensure at most one Patient resource is sent
      to the Research Platform (RP)
    - Contextualizer ([patient-deduplicator](patient-deduplicator)): Set patient references right
    - DateShifter ([date-shifter](date-shifter)): Shift dates for anonymization
    - Deidentifier ([deidentifier](deidentifier)): Main deidentification process based on white-list approach of
      filtering, hashing, redacting and data-shifting.
    - Harmonizer ([harmonizer](harmonizer)): Map to standard ontologies by
      accessing [dataprovision-qomop-service](https://github.com/smart4health/dataprovision-qomop-service)
- Upload result to Research Platform ([uploader](uploader))

## Local Deployment

```shell script
docker run --rm -it --name postgres -e POSTGRES_PASSWORD=password -e POSTGRES_USER=username -e POSTGRES_DB=development -p 5432:5432 postgres
SPRING_PROFILES_ACTIVE=local-rp,postgres ./gradlew bootRun
```

### Test the flow

For ease of testing, use the `submitBundle` task after starting the server with the
`local-rp` profile. By default, it uses a pre-existing bundle in the jar resources, but a different file can be
specified with `--file <filename>`.

The task starts a little Netty+http4k server to listen to the output of the pipeline, and sends a proper request to the
deident-server itself. If the task is cancelled
(with Ctrl-C or similar) it may take many seconds/minutes before the background Netty server finishes, due to gradle
being a daemon. Just wait until it frees itself, or (on linux) run `sudo lsof -i -P -n | grep LISTEN`, find the port in
use (4040), and kill the process.

If the pipeline fails to send a result, the http4k server will quit after 5 seconds.

## Debugging on dev

This service never stores actual FHIR resources. That's why you can use the profile `debug-cache` for dev
environments. It will store the last `debug-cache-max-size` jobs in memory including the resources to be uploaded (
meaning they're past deident and harmonized already).

Usage to get the last job:

```shell
curl --location --request GET 'https://deident.dev.healthmetrix.com/v1/debug/cache/job/last' \
--header 'Authorization: Basic <researchapp>'
```

Also available:

- `GET /v1/debug/cache/jobs?user={theUserIdBase64}`: all jobs by userId
- `GET /v1/debug/cache/job/{jobId}`: job by jobId (accessible in logs)
- `POST /v1/debug/cache/clear`: clears the cache

Obviously this is only for testing and should not be active on any environments with real data (although they're
anonymized already).

## User Statistics

The app can fetch statistics about the resources it provided already and also the global number of users. See statistics
module or /v1/stats endpoint in the swagger docs.

## Swagger documentation

Accessible at `{host}/docs-ui.html`

## JSON Filter Settings

The files that make up the JSON filter settings, and the rules inside, are applied in order, output might change if they
are reordered. As rules are changed in the future, please move more into their own resource-type-prefixed files

## date_shift_paths.json

A list of fhir paths pointing to dates to consider when date shifting.

It is for now duplicated with the json filter settings, because they should also be kept in the output.

## Hashicorp Vault local deployment

To run Vault locally and let deident use approle authentication using sample secrets, proceed the following:

1. Start Vault dev server locally (dev mode skips unsealing and some defaults):

```shell
docker run --rm -p 8200:8200 --cap-add=IPC_LOCK --name=vault-dev -e 'VAULT_DEV_ROOT_TOKEN_ID=root' vault
```

2. Run this custom set up script for the initial token, policies, roles and secrets. Also starts the service which will
   use the initial token to renew the approle lease every 20 seconds and using full pull mode to fetch the role-id and
   secret-id:

```shell
SPRING_CLOUD_VAULT_TOKEN=$(sh vault_local.sh | tail -1) SPRING_PROFILES_ACTIVE=secrets-vault ./gradlew bootRun
```
