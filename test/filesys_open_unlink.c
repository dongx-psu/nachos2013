#include "filesysgrader.h"
#include "stdio.h"
#include "stdlib.h"

#define count 10
int buffer[count];

int main(int argc, char** argv)
{
    int size = getFreeDiskSize();
    int fp = creat("file1");
    int i;
    for (i = 0; i < count; i++)
        buffer[i] = i;
    assertTrueWMsg(write(fp, buffer, count * 4) == count * 4, "write failed");
    for (i = 0; i < count; i++)
        buffer[i] = 0;
    close(fp);

    fp = open("file1");
    assertTrueWMsg(read(fp, buffer, count * 4) == count * 4, "read failed");
    for (i = 0; i < count; i++)
        assertTrueWMsg(buffer[i] == i, "read wrong content");

    unlink("file1");
    close(fp);
    assertTrueWMsg(size == getFreeDiskSize(), "mismatch size");
    done();
    return 0;
}
