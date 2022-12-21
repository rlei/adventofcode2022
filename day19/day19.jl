using Pkg
Pkg.add(["JuMP", "HiGHS"])

using JuMP
using HiGHS

struct ModelParam
    blue_print_no::Int
    ore_robot_ore_cost::Int
    clay_robot_ore_cost::Int
    obsidian_robot_ore_cost::Int
    obsidian_robot_clay_cost::Int
    geode_robot_ore_cost::Int
    geode_robot_obsidian_cost::Int
end

function extractParams(strNums)
    return ModelParam(
        parse(Int, strNums[1]),
        parse(Int, strNums[2]),
        parse(Int, strNums[3]),
        parse(Int, strNums[4]),
        parse(Int, strNums[5]),
        parse(Int, strNums[6]),
        parse(Int, strNums[7]),
    )
end

function solve(params::ModelParam, minutes::Int)
    model = Model(HiGHS.Optimizer)

    @variable(model, ore_robot[1:minutes] >= 1, Int)
    @variable(model, clay_robot[1:minutes] >= 0, Int)
    @variable(model, obsidian_robot[1:minutes] >= 0, Int)
    @variable(model, geode_robot[1:minutes] >= 0, Int)

    @variable(model, new_ore_robot[1:minutes] >= 0, Int)
    @variable(model, new_clay_robot[1:minutes] >= 0, Int)
    @variable(model, new_obsidian_robot[1:minutes] >= 0, Int)
    @variable(model, new_geode_robot[1:minutes] >= 0, Int)

    @variable(model, ore_minute_remaining[1:minutes] >= 0, Int)
    @variable(model, clay_minute_remaining[1:minutes] >= 0, Int)
    @variable(model, obsidian_minute_remaining[1:minutes] >= 0, Int)
    @variable(model, geode_minute_remaining[1:minutes] >= 0, Int)

    @variable(model, ore_minute_begin[1:minutes] >= 0, Int)
    @variable(model, clay_minute_begin[1:minutes] >= 0, Int)
    @variable(model, obsidian_minute_begin[1:minutes] >= 0, Int)
    @variable(model, geode_minute_begin[1:minutes] >= 0, Int)

    @constraint(model, ore_robot[1] == 1)
    @constraint(model, clay_robot[1] == 0)
    @constraint(model, obsidian_robot[1] == 0)
    @constraint(model, geode_robot[1] == 0)

    @constraint(model, new_ore_robot[1] == 0)
    @constraint(model, new_clay_robot[1] == 0)
    @constraint(model, new_obsidian_robot[1] == 0)
    @constraint(model, new_geode_robot[1] == 0)

    @constraint(model, ore_minute_begin[1] == 0)
    @constraint(model, ore_minute_remaining[1] == 0)
    @constraint(model, clay_minute_begin[1] == 0)
    @constraint(model, clay_minute_remaining[1] == 0)
    @constraint(model, obsidian_minute_begin[1] == 0)
    @constraint(model, obsidian_minute_remaining[1] == 0)
    @constraint(model, geode_minute_begin[1] == 0)
    @constraint(model, geode_minute_remaining[1] == 0)

    for d = 2:minutes
        @constraint(model, ore_robot[d] == ore_robot[d-1] + new_ore_robot[d-1])
        @constraint(model, clay_robot[d] == clay_robot[d-1] + new_clay_robot[d-1])
        @constraint(model, obsidian_robot[d] == obsidian_robot[d-1] + new_obsidian_robot[d-1])
        @constraint(model, geode_robot[d] == geode_robot[d-1] + new_geode_robot[d-1])
        @constraint(model, new_ore_robot[d] + new_clay_robot[d] + new_obsidian_robot[d] + new_geode_robot[d] <= 1)

        @constraint(model, ore_minute_begin[d] == ore_minute_remaining[d-1] + ore_robot[d-1])
        @constraint(model, ore_minute_remaining[d] == ore_minute_begin[d]
                                             - new_ore_robot[d] * params.ore_robot_ore_cost
                                             - new_clay_robot[d] * params.clay_robot_ore_cost
                                             - new_obsidian_robot[d] * params.obsidian_robot_ore_cost
                                             - new_geode_robot[d] * params.geode_robot_ore_cost)

        @constraint(model, clay_minute_begin[d] == clay_minute_remaining[d-1] + clay_robot[d-1])
        @constraint(model, clay_minute_remaining[d] == clay_minute_begin[d] - new_obsidian_robot[d] * params.obsidian_robot_clay_cost)

        @constraint(model, obsidian_minute_begin[d] == obsidian_minute_remaining[d-1] + obsidian_robot[d-1])
        @constraint(model, obsidian_minute_remaining[d] == obsidian_minute_begin[d] - new_geode_robot[d] * params.geode_robot_obsidian_cost)

        @constraint(model, geode_minute_begin[d] == geode_minute_remaining[d-1] + geode_robot[d-1])
        @constraint(model, geode_minute_remaining[d] == geode_minute_begin[d])
    end

    # Note: geode_minute_remaining[N] does NOT include Nth minute's product
    @objective(model, Max, geode_minute_begin[minutes] + geode_robot[minutes])

    optimize!(model)

    # print(solution_summary(model, verbose=true))
    # print(solution_summary(model))
    max_geodes = objective_value(model)
    println("max geodes ", max_geodes)
    max_geodes_int = round(Int, max_geodes)
    return max_geodes_int
end

re = r"Blueprint (\d+).*ore robot.*?(\d+).*?clay robot.*?(\d+).*obsidian robot.*?(\d+).*?(\d+).*geode robot.*?(\d+).*?(\d+)"

params = map(line -> extractParams(match(re, line).captures[1:7]), chomp.(readlines()))
println(params)

part1 = sum([a*b for (a,b) in zip(map(p -> solve(p, 24), params), map(p -> p.blue_print_no, params))])
part2 = reduce(*, map(p -> solve(p, 32), params[1:3]))

println("Part #1 result ", part1)
println("Part #2 result ", part2)

exit(0)
