#include <algorithm>
#include <array>
#include <bitset>
#include <cstring>
#include <iostream>
#include <tuple>
#include <vector>

const std::size_t MAX_WIDTH = 7;

using std::array, std::bitset, std::string, std::vector;
using std::cout, std::endl;

typedef bitset<MAX_WIDTH> Row;

struct Bitmap {
    vector<Row> bitmap;
    const int _width;

    void grow_by(size_t rows) {
        for (size_t i = 0; i < rows; i++) {
            bitmap.push_back(Row());
        }
    }

    constexpr size_t width() const noexcept {
        return _width;
    }

    size_t height() const noexcept {
        return bitmap.size();
    }

    // Checks if the spirit has any overlapping pixel with this one, if being placed
    // at (left, top). Keep in mind all bitmaps are stored bottom up.
    bool intersects(const Bitmap &spirit, size_t left, size_t top) const noexcept {
        // note bitmaps are stored in a Y-reversed fashion
        for (int y = top, y_spr = spirit.height() - 1; y_spr >= 0; y--, y_spr--) {
            auto spr_line = spirit.bitmap[y_spr] << this->width() - spirit.width() - left;
            auto line = this->bitmap[y];
            if ((spr_line & line).any()) {
                return true;
            }
        }
        return false;
    }

    // Caller's responsibility to make sure this is a valid placement
    void place(const Bitmap &spirit, size_t left, size_t top) noexcept {
        for (int y = top, y_spr = spirit.height() - 1; y_spr >= 0; y--, y_spr--) {
            auto spr_line = spirit.bitmap[y_spr] << this->width() - spirit.width() - left;
            this->bitmap[y] |= spr_line;
        }
    }

    void dump() const noexcept {
        for (auto it = bitmap.rbegin(); it != bitmap.rend(); it++) {
            cout << *it << endl;
        }
    }
};

template< std::size_t N, std::size_t M >
Bitmap makeBlock(const array< bitset<N>, M> bit_array_map) {
    auto lines = vector<Row>();
    for (auto &&line : bit_array_map) {
        lines.push_back(Row(line.to_ulong()));
    }
    
    // lines[0] means the bottom
    std::reverse(lines.begin(), lines.end());
    return {lines, N};
}

constexpr array flat_bar{ bitset<4>{0b1111} };
constexpr array cross{ bitset<3>{0b010}, bitset<3>{0b111}, bitset<3>{0b010} };
constexpr array J{ bitset<3>{0b001}, bitset<3>{0b001}, bitset<3>{0b111} };
constexpr array vertical_bar{ bitset<1>{0b1}, bitset<1>{0b1}, bitset<1>{0b1}, bitset<1>{0b1} };
constexpr array smash_boy{ bitset<2>{0b11}, bitset<2>{0b11} };

int main(int argc, char *argv[]) {
    vector<Bitmap> blocks = {
        makeBlock(flat_bar),
        makeBlock(cross),
        makeBlock(J),
        makeBlock(vertical_bar),
        makeBlock(smash_boy),
    };

    auto chamber = Bitmap{{}, MAX_WIDTH};

    string commands;
    std::getline(std::cin, commands);

    // note the checkpoint size must be divisible by blocks.size()
    const auto checkpoint = 2000;
    const auto snapshot_rows = 7;
    // (chamber top rows pattern, blocks so far, highest rock rows, command #)
    auto chamber_top_snapshots = vector<std::tuple<vector<Row>, uint64_t, u_int64_t, int>>();
    bool fast_forwarded = false;

    auto remaining_blocks = (argc > 1 && strcmp(argv[1], "-2") == 0) ? 1000000000000ull : 2022ull;
    auto blocks_fall = 0ull;
    auto skipped_rows = 0ull;
    int command_no = 0;
    int block_no = 0;
    auto highest_rock_rows = 0;
    while (blocks_fall++ != remaining_blocks) {
        auto& next_block = blocks[block_no];
        block_no = (block_no + 1) % blocks.size();

        auto new_chamber_rows = highest_rock_rows + next_block.height() + 3;
        if (new_chamber_rows > chamber.bitmap.size()) {
            chamber.grow_by(new_chamber_rows - chamber.bitmap.size());
        }

        int block_x = 2;
        int block_y = new_chamber_rows - 1;

        for (;;) {
            switch (commands[command_no]) {
            case '>': {
                int to_right = block_x + 1;
                if (to_right + next_block.width() <= chamber.width() &&
                    !chamber.intersects(next_block, to_right, block_y)) {
                    block_x = to_right;
                }
                break;
            }
            case '<': {
                int to_left = block_x - 1;
                if (to_left >= 0 &&
                    !chamber.intersects(next_block, to_left, block_y)) {
                    block_x = to_left;
                }
                break;
            }
            default:
                std::cerr << "Unknown command " << commands[command_no] << endl;
                break;
            }
            command_no = (command_no + 1) % commands.size();

            if (block_y >= (int)next_block.height() && !chamber.intersects(next_block, block_x, block_y - 1)) {
                block_y--;
            } else {
                chamber.place(next_block, block_x, block_y);
                break;
            }
        }
        // cout << "Chamber: " << endl;
        // chamber.dump();
        for (int y = chamber.height() - 1; y >= 0; y--) {
            if (chamber.bitmap[y].any()) {
                highest_rock_rows = y + 1;
                break;
            }
        }
        if (!fast_forwarded && (blocks_fall % checkpoint) == 0) {
            vector<Row> snapshot;
            std::copy(chamber.bitmap.begin() + (highest_rock_rows - snapshot_rows), chamber.bitmap.begin() + highest_rock_rows,
                      std::back_inserter(snapshot));
            Row full = ~Row();
            auto full_row_near_top = std::find(snapshot.begin(), snapshot.end(), full);
            if (full_row_near_top != snapshot.end()) {
                cout << "full row near top after blocks " << blocks_fall
                     << "; command # " << command_no << "; block # " << block_no << endl;
                bool matched = false;
                for (int i = chamber_top_snapshots.size() - 1; i >= 0; i--) {
                    if (std::get<0>(chamber_top_snapshots[i]) == snapshot &&
                        command_no == std::get<3>(chamber_top_snapshots[i])) {
                        cout << "==> matched chamber pattern with snapshot " << i << " of "
                             << chamber_top_snapshots.size() << endl
                             << "prev command # " << std::get<3>(chamber_top_snapshots[i])
                             << "; prev blocks " << std::get<1>(chamber_top_snapshots[i]) << endl
                             << "prev highest_rock_rows " << std::get<2>(chamber_top_snapshots[i])
                             << "; current highest_rock_rows " << highest_rock_rows << endl;
                        for (auto it = snapshot.rbegin(); it != snapshot.rend(); it++) {
                            cout << *it << endl;
                        }
                        auto recurring_every_blocks = blocks_fall - std::get<1>(chamber_top_snapshots[i]);
                        auto recurring_every_rows = highest_rock_rows - std::get<2>(chamber_top_snapshots[i]);
                        // fast forward
                        auto ff_times = (remaining_blocks - blocks_fall) / recurring_every_blocks;
                        if (ff_times) {
                            fast_forwarded = true;
                            skipped_rows += recurring_every_rows * ff_times;
                            remaining_blocks -= recurring_every_blocks * ff_times;
                            cout << "Chamber top repeating every blocks " << recurring_every_blocks
                                 << " and rows " << recurring_every_rows << endl
                                 << "fast forward blocks " << recurring_every_blocks * ff_times
                                 << "; fast forward rows " << skipped_rows
                                 << "; remaining blocks " << remaining_blocks - blocks_fall << endl;
                            // otherwise, remaining blocks is too few to skip
                        }
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    chamber_top_snapshots.push_back(std::make_tuple(snapshot, blocks_fall, highest_rock_rows, command_no));
                }
            }
        }
    }
    cout << "Final rock height " << highest_rock_rows + skipped_rows << endl;
    return 0;
}