use bitvec::vec::BitVec;
use clap::Parser;
use itertools::Itertools;
use regex::Regex;
use std::collections::{BinaryHeap, HashMap, HashSet};
use std::io::Write;
use std::{cmp, io};

type ValveId = u8;

#[derive(Debug)]
struct Valve {
    id: ValveId,
    rate: u16,
    edges: Vec<ValveId>,
}

#[derive(Parser, Debug)]
#[command(author, about, long_about = None)]
struct Args {
    #[arg(short, long, default_value_t = 30)]
    minutes: u16,

    /// Solves problem 2
    #[arg(long, default_value_t = false)]
    prob2: bool,

    /// Being chatty
    #[arg(short, long, default_value_t = false)]
    verbose: bool,
}

fn main() {
    let args = Args::parse();
    let minutes = args.minutes;
    println!("Remaining minutes: {}", minutes);

    let re =
        Regex::new(r"Valve (\S+) has flow rate=(\d+); tunnels? leads? to valves? (.*)").unwrap();

    let valves: Vec<_> = io::stdin()
        .lines()
        .map(|line| {
            let l = line.unwrap();
            let cap = re.captures(l.as_str()).unwrap();
            let name = cap[1].to_string();
            let rate = cap[2].parse::<u16>().unwrap();
            let leads_to: Vec<_> = cap[3].split(", ").map(|s| s.to_owned()).collect();

            if args.verbose {
                println!("Valve: {} rate: {} leads to {:?}", name, rate, &leads_to);
            }
            (name, rate, leads_to)
        })
        .collect();

    let valves_to_id: HashMap<String, ValveId> =
        valves.iter().map(|v| v.0.clone()).zip(0..).collect();
    // let id_to_valves: HashMap<_, _> = valves_to_id.iter().map(|(k, v)| (*v, k)).collect();

    let graph: HashMap<_, _> = valves
        .into_iter()
        .map(|valve| {
            let valve_id = Valve {
                id: valves_to_id[&valve.0],
                rate: valve.1,
                edges: valve.2.iter().map(|name| valves_to_id[name]).collect(),
            };
            (valve_id.id, valve_id)
        })
        .collect();

    let start_valve = valves_to_id["AA"];
    let good_valves: Vec<_> = graph
        .values()
        .filter(|valve| valve.rate > 0)
        .map(|v| v.id)
        .collect();
    let good_valves_distances = find_valves_distances(&graph, start_valve, &good_valves);

    if !args.prob2 {
        let max_pressure = find_max_pressure(
            &graph,
            start_valve,
            &good_valves,
            &good_valves_distances,
            minutes,
            args.verbose,
        );
        println!("Most pressure {:?}", max_pressure);
    } else {
        let mut progress = 0;

        let subsets: Vec<_> = good_valves.iter().cloned().powerset().collect();
        if args.verbose {
            println!("{} valves subsets to examine", subsets.len());
        }
        let subsets_max_results: HashMap<_, _> = subsets
            .iter()
            .map(|subset| {
                if args.verbose {
                    let percent_before = progress * 100 / subsets.len();
                    progress += 1;
                    let percent_now = progress * 100 / subsets.len();
                    if percent_before != percent_now {
                        print!(".");
                        if percent_now % 10 == 0 {
                            print!("{}%", percent_now);
                        }
                        io::stdout().flush().unwrap();
                    }
                }
                let max = find_max_pressure(
                    &graph,
                    start_valve,
                    &subset,
                    &good_valves_distances,
                    minutes,
                    false,
                );
                (valves_to_bitmask(subset), max)
            })
            .collect();
        if args.verbose {
            println!();
        }

        let good_valves_bv = valves_to_bitmask(&good_valves);
        let (max1, max2) = subsets
            .into_iter()
            .map(|subset| {
                let subset_bv = valves_to_bitmask(&subset);
                let the_others = good_valves_bv.clone() ^ subset_bv.clone();

                let res1 = subsets_max_results.get(&subset_bv).unwrap();
                let res2 = subsets_max_results.get(&the_others).unwrap();
                (res1, res2)
            })
            .max_by_key(|(max1, max2)| max1.released + max2.released)
            .unwrap();
        println!("{:?}\n{:?}", max1, max2);
        println!("Most pressure {}", max1.released + max2.released);
    }
}

fn valves_to_bitmask(valves: &Vec<ValveId>) -> BitVec<u64> {
    let bits = valves
        .iter()
        .map(|v| 1u64 << v)
        .reduce(|a, b| a | b)
        .unwrap_or(0);
    BitVec::<_>::from_element(bits)
}

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
struct State {
    opened: Vec<ValveId>,
    released: u16,
    remaining_minutes: u16,
}

fn find_max_pressure(
    graph: &HashMap<ValveId, Valve>,
    start_valve: ValveId,
    good_valves: &Vec<ValveId>,
    good_valves_distances: &HashMap<(ValveId, ValveId), u16>,
    remaining_minutes: u16,
    verbose: bool,
) -> State {
    let mut visited_states = vec![State {
        opened: vec![start_valve],
        released: 0,
        remaining_minutes,
    }];

    let mut max_path: Option<State> = None;
    let mut max_pressure = 0;

    loop {
        let mut next_visited_states = Vec::new();
        let visited_states_len = visited_states.len();

        for state in visited_states.into_iter() {
            let mut next_moves = Vec::new();
            for v in good_valves.iter() {
                if !state.opened.contains(v) {
                    // +1 for opening the Valve
                    let minutes_took =
                        good_valves_distances[&(*state.opened.last().unwrap(), *v)] + 1;
                    if state.remaining_minutes < minutes_took {
                        continue;
                    }
                    let remaining = state.remaining_minutes - minutes_took;

                    let mut now_opened = Vec::with_capacity(state.opened.len() + 1);
                    now_opened.extend(state.opened.iter());
                    now_opened.push(*v);

                    next_moves.push(State {
                        opened: now_opened,
                        released: state.released + graph[v].rate * remaining,
                        remaining_minutes: remaining,
                    });
                }
            }
            if next_moves.is_empty() {
                // no more move for this state
                if state.released > max_pressure {
                    max_pressure = state.released;
                    max_path = Some(state);
                }
            } else {
                next_visited_states.extend(next_moves);
            }
        }
        if verbose {
            println!(
                "{} visited states, {} next states",
                visited_states_len,
                next_visited_states.len()
            );
        }
        if next_visited_states.is_empty() {
            // all possible paths (within the time bound) have been traversed
            break;
        }
        visited_states = next_visited_states;
    }
    max_path.unwrap_or(State {
        opened: vec![],
        released: 0,
        remaining_minutes,
    })
}

fn find_valves_distances(
    graph: &HashMap<ValveId, Valve>,
    start_valve: ValveId,
    good_valves: &Vec<ValveId>,
) -> HashMap<(ValveId, ValveId), u16> {
    let mut result = HashMap::new();
    good_valves.iter().for_each(|valve| {
        let distance = dijkstra_shortest_distance(graph, start_valve, *valve);
        result.insert((start_valve, *valve), distance);
        // println!("{} to {}: {}", start_valve, *valve, distance);
    });
    good_valves.iter().combinations(2).for_each(|pair| {
        let (from, to) = (pair[0], pair[1]);
        let distance = dijkstra_shortest_distance(graph, *from, *to);
        result.insert((*from, *to), distance);
        result.insert((*to, *from), distance);
        // println!("{} to {}: {}", from, to, distance);
    });
    result
}

fn dijkstra_shortest_distance(graph: &HashMap<ValveId, Valve>, from: ValveId, to: ValveId) -> u16 {
    let mut cost_map: HashMap<_, _> = graph.keys().map(|valve| (*valve, std::u16::MAX)).collect();
    let mut visited = HashSet::with_capacity(graph.len());
    let mut heap = BinaryHeap::new();

    cost_map.insert(from, 0);
    heap.push(cmp::Reverse((0, from)));
    while heap.len() > 0 {
        let cmp::Reverse((_cost, valve)) = heap.pop().unwrap();
        visited.insert(valve);

        for next in graph[&valve].edges.iter() {
            if !visited.contains(&next) {
                let old_cost = cost_map[next];
                let new_cost = cost_map[&valve] + 1;
                if new_cost < old_cost {
                    // got the shortest distance
                    if *next == to {
                        return new_cost;
                    }
                    heap.push(cmp::Reverse((new_cost, *next)));
                    cost_map.insert(*next, new_cost);
                }
            }
        }
    }

    // impossible
    std::u16::MAX
}
