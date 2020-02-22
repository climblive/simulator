# ClimbLive Simulator

A simple but yet powerful tool for simulating climbing contests running on the ClimbLive™ platform. Built with load
testing in mind and is thus well suited for running heavy load tests useful for pinpointing performance bottlenecks.

## Build

```
$ ./gradlew uberJar
```

## Usage

Create a file `contenders.txt` with one registration code per line.

```
$ cat contenders.txt
NAGRXMAA
5A6LCF7N
F9QCQ57S
6XSASEJF
H33CNFLK
```

The simulator has many options to customize how the simulation should be run.

```
$ java -jar simulator-1.0-SNAPSHOT.jar -h
Usage: simulator [OPTIONS] input

Options:
  --api-url TEXT             API URL
  --delay-min INT            Minimum delay in milliseconds
  --delay-max INT            Maximum delay in milliseconds
  -l, --contender-limit INT  Contender limit
  -r, --run-time INT         Maximum run time in seconds
  --remove-only              Only remove ticks
  --force-register           Force register even if already registered
  -h, --help                 Show this message and exit

Arguments:
  input  Registration codes
```

### Example

To simulate a contest with 500 contenders at a normal pace during 1 minute.

```
$ java -jar simulator-1.0-SNAPSHOT.jar --delay-min=10000 --delay-max=300000 --run-time 60 contenders.txt
```

The simulator will continuously print out the number of operations per second and the average latency. After the
simulation is completed a full summary is printed out like the one below.

```
╒══════════════════════════════════╤═════════╤══════╤════════╤═══════════╕
│ Operation                        │ Samples │ Min  │ Max    │ Average   │
╞══════════════════════════════════╪═════════╪══════╪════════╪═══════════╡
│ GET /contender/findByCode?code=? │ 560     │ 20ms │ 1940ms │ 1080.18ms │
╞══════════════════════════════════╪═════════╪══════╪════════╪═══════════╡
│ GET /compClass                   │ 560     │ 21ms │ 2044ms │ 874.15ms  │
╞══════════════════════════════════╪═════════╪══════╪════════╪═══════════╡
│ GET /tick                        │ 560     │ 20ms │ 2075ms │ 1054.30ms │
╞══════════════════════════════════╪═════════╪══════╪════════╪═══════════╡
│ GET /problem                     │ 560     │ 20ms │ 1579ms │ 1163.05ms │
╞══════════════════════════════════╪═════════╪══════╪════════╪═══════════╡
│ PUT /contender/?                 │ 1325    │ 21ms │ 162ms  │ 40.32ms   │
╞══════════════════════════════════╪═════════╪══════╪════════╪═══════════╡
│ POST /tick                       │ 1316    │ 23ms │ 236ms  │ 41.66ms   │
╞══════════════════════════════════╪═════════╪══════╪════════╪═══════════╡
│ DELETE /tick/?                   │ 1268    │ 22ms │ 230ms  │ 40.69ms   │
╞══════════════════════════════════╪═════════╪══════╪════════╪═══════════╡
│ PUT /tick/?                      │ 1291    │ 23ms │ 203ms  │ 42.16ms   │
╞══════════════════════════════════╪═════════╪══════╪════════╪═══════════╡
│ Total                            │ 7440    │ 20ms │ 2075ms │ 342.80ms  │
╘══════════════════════════════════╧═════════╧══════╧════════╧═══════════╛
```

The latency is significantly higher for the first 4 operations in the table since no delays are applied during the
initial data loading and registration phase.