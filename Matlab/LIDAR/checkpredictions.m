% Check how good predictions are vs simply using last position
function checkpredictions(csnap)
  data={};
  for i=1:length(csnap)
    for j=1:length(csnap(i).tracker.tracks)
      t=csnap(i).tracker.tracks(j);
      if t.trackedBy == csnap(i).vis.unit
        data{t.id}(i)=t;
      end
    end
  end
  minscanpts=4;
  z=[];p=[];
  for i=1:length(data)
    prederr=[]; zoherr=[]; scanpts=[];frameid=[];
    for j=1:length(data{i})
      if isempty(data{i}(j).id) || data{i}(j).age<20
        continue;
      end
      t=data{i}(j);
      prederr(end+1,:,:)=t.legs-t.predictedlegs;
      zoherr(end+1,:,:)=t.legs-t.prevlegs;
      scanpts(end+1,:)=[length(t.scanpts{1}),length(t.scanpts{2})];
      frameid(end+1,:)=[i,j];
    end
    if size(scanpts,1)<10
      continue;
    end
    sel=scanpts(:,1)>=minscanpts & scanpts(:,2)>=minscanpts;
    if sum(sel)>100
      fprintf('Person %2d, %4d/%4d frames, RMS prederr=[%5.3f %5.3f %5.3f %5.3f], zoherr=[%5.3f %5.3f %5.3f %5.3f]\n', i, sum(sel), length(data{i}), sqrt(mean(prederr(sel,:,:).^2)), sqrt(mean(zoherr(sel,:,:).^2)));
    end
    p=[p;prederr(sel,:,:)];
    z=[z;zoherr(sel,:,:)];
  end

  fprintf('Overall: prediction: %.4f, ZOH: %.4f\n', sqrt(mean(p(:).^2)), sqrt(mean(z(:).^2)));
  
  setfig('predictions');clf;
  subplot(211);
  pdfplot(p(p(:)~=0));
  hold on;
  pdfplot(z(z(:)~=0));
  c=axis;axis([-0.1 0.1 0 c(4)]);
  title('Prediction Error');
  xlabel('Error');
  legend('Predicted','ZOH');

  subplot(212);
  cdfplot(abs(p(p(:)~=0)));
  hold on; 
  cdfplot(abs(z(z(:)~=0)));
  xlabel('Error');
  cut=prctile(z(:),99);
  axis([0 0.05 0 1]);
  grid on;
end

