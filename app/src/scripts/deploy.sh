#!/system/bin/sh
p=$(pm path $1)
p=${p#package:}
echo "exec /system/bin/app_process -Djava.class.path=$p / io.github.a13e300.intenttracker.cli.MainKt "'$*' > /data/local/tmp/itc
chmod +x /data/local/tmp/itc
