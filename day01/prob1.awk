BEGIN { max = 0; currentElf = 0 }
{
	if (NF == 0) {
		if (currentElf > max) {
			max = currentElf;
		}
		currentElf = 0;
	} else {
		currentElf += $1;
	}
}
END { print "Max Calories:", max }
