# Clinical Knowledge Verification

A service for loading NLP extracted data that provides an GUI for qualified medical professionals to verify the validity of the extracted data.

## Motivation

All this machine extracted data needs to be 100% kosher so our patients don't have problems.

## Running the service 

On a unix machine with docker installed and credential added, this service can be run as follows:

```shell
sbt "startDynamodbLocal" "run"
sbt "startDynamodbLocal ~re-start"
```

## API Reference

Rest doc for this api is accessible at  `[Service-Host]:[Service-port]/swagger-ui.html`. 
As an example, if the service was executed with the `make run` command, the rest doc is accessible at `http://localhost:8101/swagger-ui.html`.
For a quick look at the documentation, without the need for your own running copy, you may use the [running dev instance doc](http://dev-clinical-leaflets.babylontech.co.uk/swagger-ui.html).  

## Tests

Need to be implemented.

## Contributors

* [Szymon Wartak](email:szymon.wartak@babylonhealth.com)

## License

Just permitted for internal use by babylonhealth.

## Questions? 

Email szymon.wartak@babylonhealth.com
