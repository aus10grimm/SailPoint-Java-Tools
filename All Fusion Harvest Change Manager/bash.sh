#!/bin/bash
 ./iiq console < getRoleList.txt
sed -i -e 's/^/delete bundle /' roleList.txt
./iiq console < roleList.txt
./iiq console < deleteEntitlements.txt
./iiq console < import.txt
