const std = @import("std");

pub fn main() !void {
    const stdin = std.io.getStdIn().reader();
    const stdout = std.io.getStdOut().writer();

    var buffer: [1]u8 = undefined;

    var ring: [4]u5 = undefined;
    // u2 is perfect for 0-3!
    var head: u2 = 0;
    var tail: u2 = 0;
    var ringSize: usize = 0;

    var pos: usize = 0;
    while (true) {
        const read_result = try stdin.read(buffer[0..1]);
        if (read_result == 0) break;
        var ch = buffer[0];
        if ((ch < 'a') or (ch > 'z')) {
            std.debug.print("ignoring invalid input char of value {d}.\n", .{ch});
            continue;
        }
        if (ringSize == 4) {
            const one: i32 = 1;
            const bitSet = one << ring[0] | one << ring[1] | one << ring[2] | one << ring[3];

            if (@popCount(bitSet) == 4) {
                try stdout.print("First marker at {d}\n", .{pos});
                break;
            }

            // drop head, let "+%" do the possible wrapping!
            head = head +% 1;
        } else {
            ringSize += 1;
        }
        // we've already checked ch is between 'a' - 'z'
        ring[tail] = @intCast(u5, ch - 'a');

        tail +%= 1;
        pos += 1;
    }
}
