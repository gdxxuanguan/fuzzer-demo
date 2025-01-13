# fuzzer-demo: 覆盖率引导的变异式模糊测试工具
## 项目概述
`fuzzer-demo` 是一个基于覆盖率引导的变异式模糊测试工具的简化实现。该工具参考了 AFL++ 的设计，使用 Java 语言开发，旨在演示模糊测试的基本组件和工作流程。工具包含以下主要组件：

1. **插装组件**（使用 AFL++ 的 `afl-cc` 实现）
2. **测试执行组件**
3. **执行结果监控组件**
4. **变异组件**
5. **种子排序组件**
6. **能量调度组件**
7. **评估组件**

## 项目结构
```plain
fuzzer-demo/
├── README.md
├── devlog.md
├── pom.xml
├── run_result        # 用于存放24h运行结果，存于docker中，未上传到GitHub
├── graphic_result    # 用于存放生成的图像结果
├── src
│   ├── main
│   │   └── java
│   │       └── edu
│   │           └── nju
│   │               └── isefuzz
│   │                   ├── energyScheduler
│   │                   │   └── EnergyScheduler.java
│   │                   ├── Evaluation
│   │                   │   ├── EvaluationComponent.java
│   │                   │   └── EvaluationTableComponent.java
│   │                   ├── executor
│   │                   │   ├── ExecutorUtils.java
│   │                   │   └── SeedHandler.java
│   │                   ├── model
│   │                   │   ├── ExecutionResult.java
│   │                   │   └── Seed.java
│   │                   ├── mutator
│   │                   │   ├── mutators
│   │                   │   └── MutatorUtils.java
│   │                   ├── seedSorter
│   │                   │   ├── Comparator
│   │                   │   ├── SeedSorter.java
│   │                   │   └── SortingStrategy.java
│   │                   └── util
│   │                   └── CoverageBasedMutationFuzzer.java
└── resources
    ├── cpptest
    │   ├── coverage_collector
    │   └── coverage_collector.cpp
    ├── targets
    └── testcases
    
```

## 目录说明
## main 模块
### energyScheduler
`edu.nju.isefuzz.energyScheduler.EnergyScheduler.java`

用于管理和调度模糊测试中种子的能量。它通过分配初始能量、消耗能量、恢复能量和根据执行结果调整能量来控制种子的使用和变异。其主要功能包括：

1. **能量分配与消耗**：为每个种子分配初始能量，并根据变异消耗能量。
2. **能量恢复**：定期恢复种子的能量，确保能量不会耗尽。
3. **根据执行结果调整能量**：奖励成功发现新覆盖块的种子，惩罚导致崩溃的种子。
4. **变异生成控制**：根据剩余能量决定可以生成多少个变异种子。

### Evaluation
#### EvaluationComponent
`edu.nju.isefuzz.Evaluation.EvaluationComponent.java`

负责生成统计图表和评估模糊测试的结果。该组件收集覆盖率数据、执行速度等信息，并生成可视化的评估报告。

#### EvaluationTableComponent
`edu.nju.isefuzz.Evaluation.EvaluationTableComponent.java`

用于展示评估数据的表格组件，提供清晰的统计数据展示，便于分析模糊测试的效果。

### executor
#### ExecutorUtils
`edu.nju.isefuzz.executor.ExecutorUtils.java`

提供创建和管理子进程的工具方法，用于执行模糊目标程序，并记录执行时间和次数等信息。

#### SeedHandler
`edu.nju.isefuzz.executor.SeedHandler.java`

用于处理模糊测试中种子的执行结果，更新种子的元数据（metadata）并计算其优先级分数。它的主要功能包括：

1. **更新执行信息**：每次执行时，更新种子的执行次数、覆盖块数、新的覆盖块数量以及最近执行时间。
2. **计算熵值**：如果种子的元数据中没有熵值，计算并更新其熵值，熵值用于表示种子的多样性。
3. **处理新覆盖块**：当种子发现新的覆盖块时，更新相应信息，并将其标记为“优先种子”。
4. **处理崩溃信息**：如果执行导致程序崩溃，将种子标记为崩溃状态。
5. **计算优先级分数**：调用 `PriorityCalculator` 计算种子的优先级分数并更新。

### model
#### ExecutionResult
`edu.nju.isefuzz.model.ExecutionResult.java`

定义执行结果的数据模型，包含执行过程中收集的各种统计数据，如覆盖率、执行时间等。

#### Seed
`edu.nju.isefuzz.model.Seed.java`

定义种子的基本属性和行为，包括种子的内容、优先级和其他相关信息。

### mutator
#### mutators
`edu.nju.isefuzz.mutator.mutators.Mutator.java`

实现具体的变异操作，如 bitflip、arith、havoc 等基础变异算子。该类定义了变异策略，用于生成新的测试用例。

#### MutatorUtils
`edu.nju.isefuzz.mutator.MutatorUtils.java`

提供变异操作的辅助方法，支持变异过程中的各种辅助功能，如随机数生成、字符替换等。

### seedSorter
#### Comparator
`edu.nju.isefuzz.seedSorter.Comparator.java`

定义种子比较器，用于根据特定策略对种子进行排序，包括覆盖率、执行时间和优先级三种方式

#### SeedSorter
`edu.nju.isefuzz.seedSorter.SeedSorter.java`

是一个用于管理种子并根据不同的策略进行排序的类。它使用了优先队列（`PriorityQueue`）来存储种子，并支持根据排序策略进行动态排序。

#### SortingStrategy
`edu.nju.isefuzz.seedSorter.SortingStrategy.java`

是一个枚举类，定义了三种排序策略，用于指导种子排序的方式。具体策略如下：

1. **COVERAGE**：按种子的覆盖率排序。通常，覆盖率较高的种子会被优先处理，因为它们可能带来更多的新覆盖块。
2. **EXECUTION_TIME**：按种子的执行时间排序。这个策略可能用于优化种子处理的效率，优先考虑执行时间较短的种子。
3. **PRIORITY**：按种子的优先级排序。优先级较高的种子通常是那些有潜力发现新漏洞或有较大影响力的种子。

### util
`edu.nju.isefuzz.util`

提供工具类和辅助功能，如目录操作、优先级计算、临时文件处理等，支持模糊测试工具的各个组件高效运行。

### CoverageBasedMutationFuzzer
`edu.nju.isefuzz.CoverageBasedMutationFuzzer.java`

作为主类

+ **初始化和参数解析**：
+ 从命令行参数中获取目标程序路径、初始种子路径和输出目录。
+ 确保输出目录、种子文件目录（favor）和崩溃种子目录（crashes）存在。
+ 设置用于存储测试结果的文件，并写入表头。
+ **创建和加载种子**：
+ 通过 `loadInitialSeed()` 方法加载初始种子文件。
+ 如果加载成功，将其转换为 `Seed` 对象并添加到种子处理和排序系统中。
+ **覆盖率收集和目标程序执行**：
+ 使用 `ExecutorUtils.executeCpp()` 执行目标程序，并获取覆盖率信息。
+ 在执行过程中，计算每次执行的耗时、覆盖的代码块、崩溃信息等。
+ **种子变异和执行**：
+ 在每个轮次中，根据当前选中的种子，生成变异种子并执行。
+ 变异种子通过 `MutatorUtils.mutateSeed()` 生成，且使用 `TempFileHandler` 处理生成的临时文件。
+ 执行结果通过 `ExecutionResult` 获取，并根据覆盖率和崩溃信息对种子进行标记（是否为“优先”或“崩溃”种子）。
+ **能量调度和变异数量控制**：
+ `EnergyScheduler` 类用于管理每个种子的“能量”消耗和恢复。每次变异操作都需要消耗一定的能量。
+ **动态排序和策略切换**：
+ 使用 `SeedSorter` 类根据不同的排序策略（如优先级、覆盖率、执行时间）动态调整种子的顺序。
+ 在每个轮次之后，可能会切换排序策略，例如每 100 轮切换一次策略。
+ **结果存储和统计**：
+ 每次执行后，测试信息（如执行时间、当前轮次、队列大小、已覆盖块、崩溃种子数等）会记录到输出文件中。
+ 崩溃种子和优先种子分别存储到指定目录下。
+ **队列大小过滤**：
+ 当种子队列大小超过最大限制时，自动移除优先级最低的种子，保持队列规模在设定范围内。

## resources 目录
### cpptest
包含覆盖率收集工具的源码和相关文件。

+ `coverage_collector/coverage_collector.cpp`: 覆盖率收集器的实现代码。
+ `coverage_collector.cpp`: 覆盖率收集器的主程序。

### targets
存放所有指定的模糊目标程序。每个模糊目标应按照要求进行编译和插装，确保模糊测试工具能够正确执行。

### testcases
存放模糊测试过程中生成的测试用例。这些测试用例用于评估模糊测试工具的效果和覆盖率。

## 使用方法
### 环境准备
确保您的系统环境满足以下要求：

+ **操作系统**: Ubuntu 22.04
+ **Java**: JDK 11 或更高版本
+ **构建工具**: Maven
+ **依赖安装**:

```plain
sudo apt-get update
sudo apt-get install -y libtool build-essential cmake python3 gcc llvm clang file binutils
```

+ **djpeg 依赖库**:

对于 `djpeg` 目标，需要安装 `libjpeg62-turbo` 库。可以通过以下命令安装：

```plain
wget http://ftp.de.debian.org/debian/pool/main/libj/libjpeg-turbo/libjpeg62-turbo_2.1.5-2_amd64.deb && \
dpkg -i libjpeg62-turbo_2.1.5-2_amd64.deb && \
rm -f libjpeg62-turbo_2.1.5-2_amd64.deb
```

### 编译项目
使用 Maven 进行项目编译：

```plain
cd fuzzer-demo
mvn clean install
```

### 运行模糊器
使用 `CoverageBasedMutationFuzzer` 进行模糊测试。以下是针对10个模糊目标的运行命令示例：

```plain
cd ./target/classes

java edu.nju.isefuzz.CoverageBasedMutationFuzzer /targets/xmllint /testcases/others/xml/small_document.xml ./out

java edu.nju.isefuzz.CoverageBasedMutationFuzzer /targets/readpng /testcases/images/png/not_kitty.png ./out

java edu.nju.isefuzz.CoverageBasedMutationFuzzer /targets/djpeg /testcases/images/jpeg/not_kitty.jpg ./out

java edu.nju.isefuzz.CoverageBasedMutationFuzzer /targets/readelf /testcases/others/elf/small_exec.elf ./out

java edu.nju.isefuzz.CoverageBasedMutationFuzzer /targets/nm-new /testcases/others/elf/small_exec.elf ./out             

java edu.nju.isefuzz.CoverageBasedMutationFuzzer /targets/objdump /testcases/others/elf/small_exec.elf ./out

java edu.nju.isefuzz.CoverageBasedMutationFuzzer /targets/mjs /testcases/others/js/small_script.js ./out

java edu.nju.isefuzz.CoverageBasedMutationFuzzer /targets/tcpdump /testcases/others/pcap/small_capture.pcap ./out

java edu.nju.isefuzz.CoverageBasedMutationFuzzer /targets/cxxfilt /testcases/others/text/small_cxx.txt ./out

java edu.nju.isefuzz.CoverageBasedMutationFuzzer /targets/lua /testcases/others/lua/all.lua ./out
```

#### 命令参数解释
每条运行模糊器的命令包含三个参数，具体如下：

```plain
java edu.nju.isefuzz.CoverageBasedMutationFuzzer <目标程序路径> <测试用例路径> <输出目录>
```

1. **目标程序路径 (**`**<目标程序路径>**`**)**:
    - **说明**: 被模糊测试的目标程序的路径。通常是编译后插装的可执行文件或脚本。
    - **示例**: `/targets/xmllint` 表示模糊测试 `xmllint` 程序。
2. **测试用例路径 (**`**<测试用例路径>**`**)**:
    - **说明**: 初始的测试用例文件路径，用于模糊测试的种子输入。该文件将作为模糊测试的起点，通过变异生成更多测试用例。
    - **示例**: `/testcases/others/xml/small_document.xml` 表示用于 `xmllint` 的初始 XML 文档。
3. **输出目录 (**`**<输出目录>**`**)**:
    - **说明**: 模糊测试过程中生成的测试用例、日志和其他输出数据将保存到该目录。确保该目录存在且具有写权限。
    - **示例**: `./out` 表示当前目录下的 `out` 文件夹。

### 模糊测试流程
1. **种子选取**: 初始化种子队列，将初始种子添加到队列中。
2. **种子调度**: 根据能量调度策略选择下一轮的种子输入。
3. **测试生成**: 通过变异操作生成新的测试用例。
4. **测试执行**: 执行模糊目标并记录结果。
5. **输出分析**: 分析执行结果，标记有效的测试输入。
6. **测试持久化**: 将有效种子保存到硬盘。

## 环境搭建
本项目提供了 Docker 环境，您可以通过 Docker 快速搭建所需的运行环境。

### 使用 Docker
1. **构建 Docker 镜像**:

```plain
docker build -t fuzzer-demo:latest .
```

2. **运行 Docker 容器**:

```plain
docker run -d fuzzer-demo tail -f /dev/null
```

3. **进入Docker容器**

```plain
docker exec -it <container-id> /bin/bash
```

4. **程序运行示例**

```plain
cd target/classes
chmod +x /opt/project/dev/target/classes/cpptest/coverage_collector
java edu.nju.isefuzz.CoverageBasedMutationFuzzer /targets/djpeg /testcases/images/jpeg/not_kitty.jpg ./out
```

### DockerHub
你可以通过以下命令从 Docker Hub 拉取 `fuzzer-demo` 镜像：

```bash
docker pull kaixuan77/fuzzer-demo:latest
```
也可以访问DockerHub链接：[fuzzer-demo](https://hub.docker.com/r/kaixuan77/fuzzer-demo)

### Docker 环境包含内容
+ 项目源码和依赖
+ 10 个指定的模糊目标，位于`resources`里的`targets`中
+ 每个模糊目标运行 24 小时后的测试数据，位于`run_result`文件夹中
+ 数据分析脚本，位于`Evaluation`包中

## 评估结果
模糊测试完成后，评估组件会生成一组统计图表，包括但不限于：

+ 10 张覆盖率曲线图，每张对应一个模糊目标, 位于`graphic_result`文件夹中

## 演示视频
为了帮助您更好地理解和使用工具，我们准备了一个演示视频，展示工具的使用方法和模糊测试流程。视频时长控制在 8 分钟以内，并上传至 B 站。

观看视频：[fuzzer-demo 演示视频](https://www.bilibili.com/video/BV125rNYPEwi/)



## 
