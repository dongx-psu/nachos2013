#include "filesysgrader.h"
#include "stdio.h"
#include "stdlib.h"

char buf[100];

int main(int argc, char** argv)
{
// phase 1
	assertTrueWMsg(mkdir("a") == 0,"mkdir error a(/a [0])");
	assertTrueWMsg(chdir("a") == 0,"chdir error a(/a [0])");
	assertTrueWMsg(rmdir("../a") == 0,"rmdir error a(/a [0])");
	assertTrueWMsg(mkdir("b") != 0,"mkdir error b(/a/b   /a [0] removed)");
	assertTrueWMsg(mkdir("./../a") == 0,"mkdir error a(/a [1])");
	assertTrueWMsg(mkdir("b") != 0,"mkdir error b(/a/b   /a [0] removed even exist /a [1] b can't be created)");
	assertTrueWMsg(mkdir("../a/b") == 0,"mkdir error b(/a/b the /a is /a [1])");
 	assertTrueWMsg(rmdir("b") != 0,"rmdir error b(/a/b   /a [0] removed even exist /a [1] and /a/b b can't be removed)");
	assertTrueWMsg(rmdir("../a/b") == 0,"rmdir error b(/a/b the /a is /a [1])");
	assertTrueWMsg(mkdir("../a/b") == 0,"mkdir error b(/a/b the /a is /a [1])");
	assertTrueWMsg(chdir("../a/b") == 0,"chdir error b(/a/b the /a is /a [1])");
// cleanup phase 1
        assertTrueWMsg(chdir("/") == 0,"chdir error /");
	assertTrueWMsg(rmdir("a/b") == 0,"rmdir error b");
	assertTrueWMsg(rmdir("a") == 0,"rmdir error a");
// phase 2
	assertTrueWMsg(mkdir("a") == 0,"mkdir error a(/a [0])");
	assertTrueWMsg(chdir("a") == 0,"chdir error a(/a [0])");
	assertTrueWMsg(rmdir("../a") == 0,"rmdir error a(/a [0])");
	assertTrueWMsg(mkdir("../a") == 0,"mkdir error a(/a [1])");
	assertTrueWMsg(creat("b") == -1,"creat error b(under /a [0]) shouldn't be created");
	assertTrueWMsg(creat("../a/b") != -1,"creat error b(under /a [1]) should be created");

	done();
	return 0;
}
