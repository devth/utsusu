# utsusu

utsusu lets you transfer all repositories under a given user or organization to
another user or organization, regardless of whether they live on github.com or a
GitHub Enterprise instance.

## Configuration

You may specify your configuration in a `config.edn` file at project root with
your settings. This is gitignored. If you do not, you'll be prompted for
configuration at runtime. Expected config file format:

```
{:source-domain "Source instance domain (github.com): "
 :source-user "Source user or org"
 :source-token "Source API token"
 :dest-domain "Destination instance domain (github.com): "
 :dest-user "Destination user or org"
 :dest-token "Destination API token"}
```

## Usage

`lein run`


## License

Copyright © 2013 Trevor C. Hartman

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
