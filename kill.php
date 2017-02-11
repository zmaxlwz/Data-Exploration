<?php

$address = "localhost";
$proxy_port = 4446;


/* Create a TCP/IP socket. */
$socket = socket_create(AF_INET, SOCK_STREAM, SOL_TCP) or die('Could not 
create socket.');

$result = socket_connect($socket, $address, $proxy_port) or die('Socket 
connection is failed.');

$config_json_str = "Stop";
echo config_json_str;
socket_write($socket, $config_json_str, strlen($config_json_str));

$reply = array();
$reply["success"] = 1;

echo json_encode($reply);

socket_close($socket);

?>


