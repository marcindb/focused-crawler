# Focused crawler

This is simple focused crawler. It is not optimized for production usage.

## Usage

Output graphs in DOT format for given seeds can be found in output dir (--output-dir parameter, default working directory).

### run parameters
````
  -d, --depth <value>      Depth of search (required)
  -s, --seeds <http://seed1>,<https://seed2>...
                           URL seeds (required)
  -l, --max-links <value>  Maximum number of links (required)
  --output-dir <value>     Output dir with generated graphs
  --tld <value>            Search links from given top level domain
  --contains <value>       Search links with given text
  --regexp <value>         Search links which match regexp

````

### sbt
````
aspectj-runner:runMain pl.ekodo.crawler.focused.engine.Engine -d {depth} -s {seeds} -l {maxLinks}
````


### build
````
sbt universal:stage
 ./engine/target/universal/stage/bin/engine -d {depth} -s {seeds} -l {maxLinks}
````
