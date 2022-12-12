open Lens.Infix

type monkey = {
  starting_items: int list;
  operation: int -> int;
  div_test_val: int;
  if_true_next: int;
  if_false_next: int;
};;

(*
let monkeys = [
  {
    starting_items = [79; 98];
    operation = ( * ) 19;
    div_test_val = 23;
    if_true_next = 2;
    if_false_next = 3;
  };
  {
    starting_items = [54; 65; 75; 74];
    operation = ( + ) 6;
    div_test_val = 19;
    if_true_next = 2;
    if_false_next = 0;
  };
  {
    starting_items = [79; 60; 97];
    operation = (fun n -> n * n);
    div_test_val = 13;
    if_true_next = 1;
    if_false_next = 3;
  };
  {
    starting_items = [74];
    operation = ( + ) 3;
    div_test_val = 17;
    if_true_next = 0;
    if_false_next = 1;
  };
];;
*)
let monkeys = [
  {
    starting_items = [56; 52; 58; 96; 70; 75; 72];
    operation = ( * ) 17;
    div_test_val = 11;
    if_true_next = 2;
    if_false_next = 3;
  };
  {
    starting_items = [75; 58; 86; 80; 55; 81];
    operation = ( + ) 7;
    div_test_val = 3;
    if_true_next = 6;
    if_false_next = 5;
  };
  {
    starting_items = [73; 68; 73; 90];
    operation = (fun n -> n * n);
    div_test_val = 5;
    if_true_next = 1;
    if_false_next = 7;
  };
  {
    starting_items = [72; 89; 55; 51; 59];
    operation = ( + ) 1;
    div_test_val = 7;
    if_true_next = 2;
    if_false_next = 7;
  };
  {
    starting_items = [76; 76; 91];
    operation = ( * ) 3;
    div_test_val = 19;
    if_true_next = 0;
    if_false_next = 3;
  };
  {
    starting_items = [88];
    operation = ( + ) 4;
    div_test_val = 2;
    if_true_next = 6;
    if_false_next = 4;
  };
  {
    starting_items = [64; 63; 56; 50; 77; 55; 55; 86];
    operation = ( + ) 8;
    div_test_val = 13;
    if_true_next = 4;
    if_false_next = 0;
  };
  {
    starting_items = [79; 58];
    operation = ( + ) 6;
    div_test_val = 17;
    if_true_next = 1;
    if_false_next = 5;
  };
];;

(** Return the new item value and the monkey to receive it *)
let throw_next_item item by_monkey =
  let worry_level = (by_monkey.operation item) / 3 in
  worry_level, 
    if worry_level mod by_monkey.div_test_val == 0 then by_monkey.if_true_next
    else by_monkey.if_false_next;;

type shenanigan_state = {
  all_items : int list list;
  monkey_inspect_times : int list;
}

let shenanigan_start_state = {
  all_items = List.map(fun m -> m.starting_items) monkeys;
  monkey_inspect_times = List.map (fun _ -> 0) monkeys;
}

let throw_item_and_update_state by_monkey curr_all_items item_to_throw =
  let item_to_receive, receiving_monkey = throw_next_item item_to_throw by_monkey in
  let receiving_monkey_new_items = List.nth curr_all_items receiving_monkey @ [item_to_receive] in 
    (* update receiving monkey's queue *)
    (Lens.for_list receiving_monkey ^= receiving_monkey_new_items) curr_all_items;;

let throw_items (state : shenanigan_state) monkeys nth_monkey =
  let by_monkey = List.nth monkeys nth_monkey in
  let items_to_throw = List.nth state.all_items nth_monkey in
  List.fold_left (throw_item_and_update_state by_monkey) state.all_items items_to_throw
  |> (fun new_all_items -> {
    (* current monkey's queue has been emptied *)
    all_items = (Lens.for_list nth_monkey ^= []) new_all_items;
    monkey_inspect_times = (Lens.for_list nth_monkey += List.length items_to_throw) state.monkey_inspect_times;
  });;

let range n = List.init n Fun.id;;

let throw_items_one_round (state : shenanigan_state) monkeys =
  let num_monkeys = List.length monkeys in
  range num_monkeys
  |> List.fold_left (fun curr_state nth_monkey -> throw_items curr_state monkeys nth_monkey) state;;

let final_state = range 20
  |> List.fold_left (fun state _ -> throw_items_one_round state monkeys) shenanigan_start_state

let () =
  final_state.all_items
  |> List.iter (fun items ->
    items
    |> List.map string_of_int
    |> String.concat ", "
    |> print_endline);;

let () =
  final_state.monkey_inspect_times
  |> List.iter (fun times ->
    times
    |> string_of_int
    |> print_endline);;

let () = final_state.monkey_inspect_times
  |> List.sort Stdlib.compare
  |> List.rev
  |> (fun l -> List.hd l * List.nth l 1)
  |> string_of_int
  |> print_endline;;
