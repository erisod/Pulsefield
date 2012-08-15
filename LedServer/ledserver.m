% LED server
% Receive commands via OSC, update LED's in response
function ledserver(p)
if nargin~=1
  fprintf('Usage: ledserver(p)\n');
  return;
end
s1=arduino_ip(1);

debug=1;
ignores={};

% Reload subsystem table
getsubsysaddr('LD','reload');

fprintf('Initializing OSC struct\n');
oscinit('LD');

global oscsetup;

fprintf('Instructing frontend and Matlab processor to use port %d to send us msgs\n', oscsetup.port);
oscmsgout('FE','/vis/dest/add/port',{oscsetup.port},'debug',debug);
oscmsgout('MPO','/pf/dest/add/port',{oscsetup.port,'LD'},'debug',debug);

period=0.1;   % Update period
lastupdate=0;
apps=struct('name'  ,{'Visible',  'FollowPB',  'FollowWht',' CircSeq','Freeze'  },...
            'fn'    ,{@fg_visible,@fg_follow,  @fg_follow, @fg_cseq   ,@fg_freeze},...
            'backfn',{@bg_white,  @bg_pulsebow,@bg_white,  @bg_freeze ,@bg_freeze},...
            'pos'   ,{'5/1',      '5/2',       '5/3',      '5/4',      '5/5'     });
for i=1:length(apps)
  apps(i).index=i;
end
currapp=apps(2);

info=struct('state',zeros(numled(),3),'mix',zeros(numled(),3),'prevstate',[],'layout',p.layout,'vis',[],'hypo',[],'running',true,...
            'colors',{p.colors},'back',struct('maxlev',1,'minlev',0.2,'state',zeros(numled(),3)));
latency=[];
refresh=true;
msgin=[];

while true
  % Receive any OSC messages
  maxwait=max(0.0,(lastupdate-now)*24*3600+period);
  if isempty(msgin)
    msgin=osc_recv(oscsetup.server,maxwait);
  end
  if ~isempty(msgin)
    if iscell(msgin)
      m=msgin{1};
      msgin=msgin(2:end);
    else
      m=msgin;
      msgin=[];
    end
    if debug
      %fprintf('Have %d messages, next is %s\n', length(msgin)+1, m.path);
    end
    if ~strncmp(m.path,'/vis',4) && ~strcmp(m.path,'/pf/update')
      fprintf('Got message: %s\n', formatmsg(m.path,m.data));
    end
    if strncmp(m.path,'/pf/dest/add/port',17)
      [host,port,proto]=spliturl(m.src);
      url=sprintf('%s://%s:%d',proto,host,m.data{1});
      [tdhost,tdport]=getsubsysaddr('TO');
      if length(m.data)>1
        % Add with Ident
        ident=m.data{2};
      elseif length(m.path)>=19
        ident=m.path(19:end);
      else
        fprintf('Received %s without an ident field\n', m.path);
        ident='';
      end
      oscadddest(url,ident);
      refresh=true;   % Always start with a dump
    elseif strncmp(m.path,'/led/app/buttons/',17)
      % TouchOSC multibutton controller
      fprintf('Got message: %s\n', formatmsg(m.path,m.data));
      if m.data{1}==1
        pos=m.path(18:end);
        appnum=find(strcmp(pos,{apps.pos}));
        if ~isempty(appnum)
          % Turn off old block
          oscmsgout('TO',sprintf('/led/app/buttons/%s',currapp.pos),{0},'debug',debug);
          currapp=apps(appnum);
          fprintf('Switching to app %d: %s\n',appnum,currapp.name);
          % Turn on block for current app
          oscmsgout('TO',sprintf('/led/app/buttons/%s',currapp.pos),{1},'debug',debug);
          oscmsgout('TO','/led/app/name',{currapp.name},'debug',debug);
        else
          fprintf('Attempt to switch to unsupported app %s\n', pos);
        end
      end
    elseif strcmp(m.path,'/led/pulsebow/pperiod')
      info.pulsebow.pperiod=expcontrol(m.data{1},0.1,20);
      refresh=true;
    elseif strcmp(m.path,'/led/pulsebow/cperiod')
      info.pulsebow.cperiod=expcontrol(m.data{1},1,100);
      refresh=true;
    elseif strcmp(m.path,'/led/pulsebow/pspatial')
      info.pulsebow.pspatial=expcontrol(m.data{1},2,10000);
      refresh=true;
    elseif strcmp(m.path,'/led/pulsebow/cspatial')
      info.pulsebow.cspatial=expcontrol(m.data{1},2,10000);
      refresh=true;
    elseif strcmp(m.path,'/led/stop')
      info.running=false;
      fprintf('Stopped by %s\n', m.src);
      refresh=true;
    elseif strcmp(m.path,'/led/start')
      info.running=true;
      fprintf('Started by %s\n', m.src);
      refresh=true;
    elseif strcmp(m.path,'/vis/visible')
      % Visibility info
      c=m.data{1}+1;   % Camera
      frame=m.data{2};
      sec=m.data{3};
      usec=m.data{4};
      mincorr=m.data{5};
      blob=m.data{6};
      info.vis(c,:)=double(blob);
      info.vis(c,blob==2)=nan;
      acquired=(((sec+usec/1e6)/3600-7)/24)+datenum(1970,1,1);   % Convert to matlab datenum (assuming 7 hours offset from GMT)
      latency=(now-acquired)*24*3600;
      if latency>0.4
        fprintf('Warning: latency=%.2f seconds\n', latency);
      end
    elseif strcmp(m.path,'/pf/entry')
      fprintf('Entry %d\n', m.data{3});
      if isempty(info.hypo)
        ids=[];
      else
        ids=[info.hypo.id];
      end
      if ismember(m.data{3},ids)
        fprintf('Got entry for id %d, which was already inside\n',m.data{3});
      else
        info.hypo=[info.hypo,struct('id',m.data{3},'pos',[nan,nan],'velocity',[nan,nan])];
      end
    elseif strcmp(m.path,'/pf/exit')
      fprintf('Exit %d\n', m.data{3});
      if isempty(info.hypo)
        ids=[];
      else
        ids=[info.hypo.id];
      end
      keepind=ids~=m.data{3};
      if sum(~keepind)~=1
        fprintf('Bad exit id %d while only the following ids are inside: %s\n',m.data{3},shortlist(ids));
      end
      info.hypo=info.hypo(keepind);
    elseif strcmp(m.path,'/pf/update')
      if isempty(info.hypo)
        ids=[];
      else
        ids=[info.hypo.id];
      end
      index=find(ids==m.data{3});
      if length(index)<1
        fprintf('Missed entry of ID %d\n', m.data{3});
        info.hypo=[info.hypo,struct('id',m.data{3},'pos',[m.data{4},m.data{5}],'velocity',[m.data{6},m.data{7}])];
      elseif length(index)>1
        fprintf('Have multiple copies of same ids inside; ids=%s\n', shortlist(ids));
      else
        info.hypo(index).pos=[m.data{4},m.data{5}];
        info.hypo(index).velocity=[m.data{6},m.data{7}];
      end
    elseif strcmp(m.path,'/pf/set/npeople')
      info.npeople=m.data{1};
      if info.npeople ~= length(info.hypo)
        fprintf('Number of people (%d) not matching number of tracked hypos (%d)\n', info.npeople,length(info.hypo));
      end
      if info.npeople<length(info.hypo)
        fprintf('Discarding all hypos (will restore on update)\n');
        info.hypo=info.hypo([]);
      end
    elseif strcmp(m.path,'/seq/step')
      info.stepnum=m.data{1};
    else
      if ~ismember(m.path,ignores)
        fprintf('LEDServer: Unhandled OSC message from %s: %s - ignoring from now on.\n', m.src, m.path);
        ignores{end+1}=m.path;
      end
    end
  end
  if refresh
    % Refresh UI
    oscmsgout('TO','/led/app/name',{currapp.name},'debug',debug);
    for i=1:length(apps)
      oscmsgout('TO',sprintf('/led/app/buttons/%s',apps(i).pos),{i==currapp.index},'debug',debug);
    end

    if info.running
      col='green';
    else
      col='red';
    end
    oscmsgout('TO','/led/app/buttons/color',{col},'debug',debug);
    oscmsgout('TO','/led/app/name/color',{col},'debug',debug);
    oscmsgout('TO','/led/app/title/color',{col},'debug',debug);
    if isfield(info,'pulsebow')
      oscmsgout('TO','/led/pulsebow/cspatial/value',{sprintf('%.1f',info.pulsebow.cspatial)});
      oscmsgout('TO','/led/pulsebow/pspatial/value',{sprintf('%.1f',info.pulsebow.pspatial)});
      oscmsgout('TO','/led/pulsebow/pperiod/value',{sprintf('%.1f',info.pulsebow.pperiod)});
      oscmsgout('TO','/led/pulsebow/cperiod/value',{sprintf('%.1f',info.pulsebow.cperiod)});
    end
    refresh=false;
  end
  if info.running && (now-lastupdate)*3600*24>=period
    info.prevstate=info.state;
    info.mix=0*info.prevstate;   % Default to all background  (mix=0 -> background, mix=1 -> foreground)
    info=currapp.backfn(info);   % Compute background LED colors
    info=currapp.fn(info);   % Override with foreground effect
    if any(size(info.prevstate)~=size(info.state))
      % First time -- initialize to something different from current state
      fprintf('Initializing prevstate\n');
      info.prevstate=info.state;
      info.prevstate(:)=255;
    end
    ls_updateallleds(info);
    lastupdate=now;
  end
end
end

function ls_updateleds(info)
  debug=0;
  s1=arduino_ip(0);
  cmd=[];
  for i=1:size(info.state,1)
    if any(info.state(i,:)~=info.prevstate(i,:))
      % Find next LED that is a different color
      j=find(info.state(i+1:end,1)~=info.state(i,1) | info.state(i+1:end,2)~=info.state(i,2) | info.state(i+1:end,3)~=info.state(i,3),1);
      if isempty(j)
        j=size(info.state,1)-i;
      else
        j=j-1;
      end
      cmd=[cmd,setled(s1,(i:i+j)-1,info.state(i,:),0)];
      if debug
        fprintf('%s->[%d,%d,%d]; ', shortlist(i:i+j), info.state(i,:));
      end
      info.prevstate(i:i+j,:)=info.state(i:i+j,:);  % Prevent it from being done again
    end
  end
  if ~isempty(cmd)
    if debug
      fprintf('Updated LEDs using %d bytes\n',length(cmd));
    end
    cmd=[cmd,'G'];  % Show()
    awrite(s1,cmd);
  end
end

function ls_updateallleds(info)
  debug=0;
  s1=arduino_ip(0);
  tic;
  sync(s1);
  elapsed=toc;
  % Mix background and live state
  state=info.back.state.*(1-info.mix)+info.state.*info.mix;

  cmd=setallleds(s1,state,0);
  cmd=[cmd,'G'];  % Show()
  awrite(s1,cmd);
  if debug
    fprintf('Updated all LEDs using %d bytes after wait of %.3f seconds\n',length(cmd),elapsed);
  end
end

% Update background colors of LEDs
function info=ls_pulse(info)
debug=1;
if ~isfield(info,'pulse')
  p0=(0:numled()-1)*2*pi/numled();
  info.pulse=struct('period',2.0, 'phaseshift',[p0;p0+2*pi/3;p0+4*pi/3]');
end
s1=arduino_ip(0);
t=now*24*3600;
phase=mod(t*2*pi/info.pulse.period+info.pulse.phaseshift,2*pi);
amp=info.back.minlev+(info.back.maxlev-info.back.minlev)*(sin(phase)+1)/2;
info.back.state=(amp.^2*.97+.03) * 127;   % Response is nonlinear (approx squared)
if debug
  fprintf('t=%.1f, phase=%.0f, amp(1,:)=%.2f %.2f %.2f,lev=%.0f %.0f %.0f\n',t,phase(1,1)*180/pi,amp(1,:),lev(1,:));
end
end

function info=bg_pulsebow(info)
debug=1;
if ~isfield(info,'pulsebow')
  info.pulsebow=struct('pperiod',0.3,'cperiod',20,'pspatial',250, 'cspatial',1000);
end

% Amplitude overall
p0=(0:numled()-1)*2*pi/info.pulsebow.pspatial;
pshift=[p0;p0;p0]';
c0=(0:numled()-1)*2*pi/info.pulsebow.cspatial;
cshift=[c0;c0+2*pi/3;c0+4*pi/3]';
t=now*24*3600;
pphase=mod(t*2*pi/info.pulsebow.pperiod+pshift,2*pi);
cphase=mod(t*2*pi/info.pulsebow.cperiod+cshift,2*pi);
amp=info.back.minlev+(info.back.maxlev-info.back.minlev)*(sin(pphase)+1)/2;
col=(sin(cphase)+1)/2;
info.back.state=((amp.*col).^2*.97+.03) * 127;   % Response is nonlinear (approx squared)
end

function info=fg_visible(info)
  info.mix(:)=0;

  if isempty(info.vis)
    fprintf('info.vis is empty!\n');
    return;
  end

  for i=1:size(info.vis,2)
    blocked=find(info.vis(:,i)==0);
    if ~isempty(blocked)
      info.state(i,:)=uint8(127*info.colors{min(blocked)+1});
      info.mix(i,:)=1;
    end
  end
end

function info=fg_follow(info)
  minradius=1;   % min radius where marker is on
  maxleds=15;    % Number of LEDs in marker when close to edge
  awidthmax=(2*pi*48/50)/sum(~info.layout.outsider) * maxleds;
  meanradius=(max(info.layout.active(:,2))-min(info.layout.active(:,2)))/2;
  
  info.state=info.mix;
  
  for i=1:length(info.hypo)
    h=info.hypo(i);
    pos=h.pos;
    % Angular width of person's marker
    awidth=awidthmax * min(1,(norm(pos)-minradius)/(meanradius-minradius));

    % Color of marker lights (track people)
    col=id2color(h.id,info.colors)*127;
    if norm(pos)>0.5   % At least .5m away from enter
      [angle,radius]=cart2pol(pos(:,1),pos(:,2));
      langle=cart2pol(info.layout.lpos(:,1),info.layout.lpos(:,2));
      indices = find(abs(langle-angle)<awidth/2 & ~info.layout.outsider);   % All LEDs inside active area, within awidth angle of person
      % fprintf('Angle=%.1f, RFrac=%.2f, NLed=%d\n', angle*360/pi, radius/meanradius,length(indices));
      for j=1:length(indices)
        info.state(indices(j),:)=col;
        info.mix(indices(j),:)=1;
      end
    end
  end

  % Visual feedback of how many people are inside
  % TODO - this could be in background led patterns, and also use channel map to display
  if ~isempty(info.hypo)
    ids=sort([info.hypo.id]);
  else
    ids=[];
  end

  for i=1:length(ids)
    info.state(i,:)=id2color(ids(i),info.colors)*127;
    info.mix(i,:)=1;
  end
  for i=length(ids)+1:6
    info.state(i,:)=info.colors{1}*127;
    info.mix(i,:)=1;
  end
end

function info=fg_freeze(info)
end

function info=bg_freeze(info)
end

function info=bg_white(info)
info.back.state(:)=127;
end

function info=fg_cseq(info)
% TODO
end

% Map a 0.0-1.0 to [lo,hi] using exponential
function val=expcontrol(v,lo,hi)
val=exp(v*log(hi/lo)+log(lo));
end

function v=invexpcontrol(val,lo,hi)
v=log(val/lo)/log(hi/lo);
end