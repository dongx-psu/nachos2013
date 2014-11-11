#include "filesysgrader.h"
#include "stdio.h"
#include "stdlib.h"

char buf[100];

int main(int argc, char** argv)
{
    int size = getFreeDiskSize();
    assertTrue(!mkdir("dir1"));
    chdir("dir1");   // in /dir1
    int fp1 = creat("/file1");
    assertTrueWMsg(fp1 >= 0, "creat failed");
    assertTrueWMsg(!link("/file1", "file1_link"), "link failed");
    close(fp1);
    assertTrueWMsg(!unlink("/file1"), "unlink failed");
    int fp2 = creat("/file1");
    assertTrueWMsg(fp2 >= 0, "creat failed");
    assertTrue(!mkdir("/dir2"));
    assertTrueWMsg(!link("/dir1/file1_link", "/dir2/file1_link"), "link failed 2");
    close(fp2);
    unlink("file1_link");
    unlink("/file1");
    chdir("..");
    unlink("dir2/file1_link");
    assertTrueWMsg(!rmdir("/dir1"), "rmdir dir1 failed");
    assertTrueWMsg(!rmdir("/dir2"), "rmdir dir2 failed");
    assertTrueWMsg(getFreeDiskSize() == size, "size mismatch");
    done();
    return 0;
}
