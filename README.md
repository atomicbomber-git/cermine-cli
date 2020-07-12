# cermine-cli

A CLI interface to CERMINE (https://github.com/CeON/CERMINE)
DIFFERENCES FROM THE ORIGINAL INTERFACE IN CERMINE
1. This client is able to parallelize the data extraction process.
2. This client allows you to customize the output directory.
3. This client only supports the extraction of .zones files for now.

# Usage
```$bash
java -jar cermine-cli.jar /path/to/directory-of-pdfs --output-dir /output/dir
```
