#include "filesysgrader.h"
#include "stdio.h"
#include "stdlib.h"

char buf[1024*1024*2];

int main(int argc, char** argv)
{
    int size = getFreeDiskSize();
    int count = readParameter(0);
    int times = readParameter(1);
    int fp  = creat("file1");
    assertTrueWMsg(fp >= 0, "creat failed");
    int i;
    for (i = 0; i < times; i++){
        assertTrueWMsg(write(fp, buf, count) == count, "write failed");
        printf("create %d\r\n", i);
    }
    close(fp);
    assertTrueWMsg(unlink("file1") == 0, "unlink failed");
    assertTrueWMsg(size == getFreeDiskSize(), "size mismatch");
    done();
    return 0;
}
