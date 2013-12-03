# utsusu

utsusu lets you transfer all repositories under a given organization to another
organization, regardless of whether they live on github.com or a GitHub
Enterprise instance.

It currently does not support transfering between individual users (pull
requests welcome).

## Configuration

The machine you run this on **must** have access to all configured GitHub
instances.

You may specify your configuration in a `config.edn` file at project root with
your settings. This is gitignored. If you do not, you'll be prompted for
configuration at runtime. Expected config file format:

```
{:source-domain "Source instance domain (github.com)"
 :source-org "Source organization name"
 :source-token "Source API token"
 :dest-domain "Destination instance domain (github.com)"
 :dest-org "Destination organization name"
 :dest-token "Destination API token"}
```

## Usage

`lein run`


## Reference

https://help.github.com/articles/duplicating-a-repository

## License

Copyright © 2013 Trevor C. Hartman

Distributed under the Eclipse Public License version 1.0.
