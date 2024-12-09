#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <iostream>
#include <sys/shm.h>
#include <chrono>
#include <unistd.h>

#define MAP_SIZE 65536

using namespace std;

int main(int argc, char* argv[]) {
    if (argc < 2) {
        cerr << "Usage: " << argv[0] << " <command>" << endl;
        return 1;
    }

    //const char* program = argv[1];
    //const char* input_file = argv[2];
    string command = argv[1];

    // 创建共享内存
    int shm_id = shmget(IPC_PRIVATE, MAP_SIZE, IPC_CREAT | IPC_EXCL | 0600);
    if (shm_id < 0) {
        perror("shmget");
        return 1;
    }

    // 将共享内存ID设置为环境变量
    char shm_id_str[10];
    snprintf(shm_id_str, sizeof(shm_id_str), "%d", shm_id);
    setenv("__AFL_SHM_ID", shm_id_str, 1);

    // 附加到共享内存
    auto *trace_bits = static_cast<unsigned char*>(shmat(shm_id, nullptr, 0));
    if (trace_bits == (void *)-1) {
        perror("shmat");
        return 1;
    }

    // 初始化共享内存
    std::memset(trace_bits, 0, MAP_SIZE);

    // 构建命令
    //string command = string(program) + " < " + input_file;

     // 开始计时
    auto start = chrono::high_resolution_clock::now();

    // 运行指定程序
    int result = system(command.c_str());
    result= WEXITSTATUS(result);

    // 结束计时
    auto end = chrono::high_resolution_clock::now();
    chrono::duration<double> elapsed = end - start;

    //deal error 小幅度修改了输出格式，方便match
    if (result != 0) {
        cout<<"Exit code: "<<result<<endl;
        // 处理错误，例如退出程序或采取其他措施
    }else{
        cout<<"Exit code: "<<result<<endl;
    }

    // 输出执行时间
    cout << "Execution time: " << elapsed.count() << " seconds\n";
    // 读取覆盖率信息
    for (int i = 0; i < MAP_SIZE; i++) {
        if (trace_bits[i]) {
            cout << "Block " << i << " executed " << static_cast<int>(trace_bits[i]) << " times\n";
        }
    }

    // 清理
    shmdt(trace_bits);
    shmctl(shm_id, IPC_RMID, nullptr);

    return 0;
}

