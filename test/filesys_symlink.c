#include "filesysgrader.h"
#include "stdio.h"
#include "stdlib.h"

int buf[100];

int main(int argc, char** argv)
{
  int size = getFreeDiskSize();
  assertTrue(!mkdir("/dir1"));
  chdir("/dir1");
  int fp1 = creat("/file1");
  assertTrueWMsg(fp1 >= 0, "create failed");
  assertTrueWMsg(!symlink("/file1", "/dir1/file1_link"), "symlink failed");
  int i;
  for (i = 0; i < 100; i++) buf[i] = i;
  assertTrueWMsg(write(fp1, buf, 100) == 100, "write failed");
  close(fp1);
  int fp2 = open("file1_link");
  assertTrueWMsg(read(fp2, buf, 100) == 100, "read failed");
  for (i = 0; i< 100; i++)
    assertTrueWMsg(buf[i] == i, "content mismatch");
  close(fp2);
  unlink("../file1");
  assertTrueWMsg(open("file1_link") < 0, "open deleted file");
  assertTrueWMsg(!symlink("file1_link", "/file2"), "symlink failed 2");
  assertTrueWMsg(open("/file2") < 0, "open deleted symlink file");
  unlink("/file2");
  unlink("/dir1/file1_link");
  chdir("..");  // in root
  rmdir("dir1");
  assertTrueWMsg(getFreeDiskSize() == size, "size mismatch");
  done();
  return 0;
}
