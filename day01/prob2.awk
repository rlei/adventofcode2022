# POSIX Awk doesn't have GNU Awk's asort, so a naive solution here
function insertToMax3(max3, num) {
	for (i = 0; i < 3; i++) {
		if (max3[i] < num) {
			out[i++] = num;
			break;
		} else {
			out[i] = max3[i];
		}
	}
	for (j = i; j < 3; j++) {
		out[j] = max3[j-1];
	}
	for (i = 0; i< 3; i++) {
		max3[i] = out[i]
	}
	# Awk can't return an array
}
BEGIN { numElves = 0; currentElf = 0; max3[0] = 0; max3[1] = 0; max3[2] = 0; }
{
	if (NF == 0) {
		insertToMax3(max3, currentElf);
		currentElf = 0;
	} else {
		currentElf += $1;
	}
}
END {
	print "Sum of max 3 Calories:", max3[0] + max3[1] + max3[2];
}
