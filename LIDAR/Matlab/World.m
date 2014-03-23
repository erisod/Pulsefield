classdef World < handle
  properties
    tracks;
    nextid;
    maxrange;
    debug;
  end
  
  methods
    function obj=World()
      obj.tracks=[];
      obj.nextid=1;
      obj.maxrange=7;
      obj.debug=true;
    end

    function w=clone(obj)
      w=World();
      for i=1:length(obj.tracks)
        w.tracks=[w.tracks,obj.tracks(i).clone()];
      end
      w.nextid=obj.nextid;
    end
      
    function predict(obj,nsteps,fps)
      for i=1:length(obj.tracks)
        obj.tracks(i).predict(nsteps,fps);
      end
    end
    
    function update(obj,vis,nsteps,fps)
      obj.predict(nsteps,fps);
      
      MAXSPECIAL=2;
      maxclass=max([1;vis.class]);
      if obj.debug&&maxclass>MAXSPECIAL
        fprintf('Assigning classes:  ');
        for i=MAXSPECIAL+1:maxclass
          meanpos=mean(vis.xy(vis.class==i,:),1);
          fprintf('%d@(%.2f,%.2f) ',i,meanpos);
          if any(isnan(meanpos))
            keyboard;
          end
        end
        fprintf('\n');
      end
      like=nan(length(obj.tracks),maxclass,maxclass);
      for p=1:length(obj.tracks)
        like(p,:,:)=obj.tracks(p).getclasslike(vis);
        if obj.debug
          fprintf('Person %2d: %s\n', obj.tracks(p).id, sprintf('%.1f ', like(p,:,:)));
        end
      end

      % Greedy assignment
      assign=nan(maxclass,2);
      for k=1:length(obj.tracks)
        [minlike,maxind]=min(like(:));
        [p,i,j]=ind2sub(size(like),maxind);
        assign(p,:)=[i,j];
        obj.tracks(p).update(vis,i,j,nsteps,fps);
        if obj.debug
          fprintf('Assigned classes %d,%d to person %d with loglike=%f\n', i,j,obj.tracks(p).id,-minlike);
        end
        like(p,:,:)=inf;
        if i>1
          like(:,i,:)=inf;
          like(:,:,i)=inf;
        end
        if j>1
          like(:,j,:)=inf;
          like(:,:,j)=inf;
        end
      end
      % Deal with leftover classes
      otherclasses=setdiff([vis.class],assign(:));
      otherclasses=otherclasses(otherclasses>MAXSPECIAL);
      if ~isempty(otherclasses)
        for i=1:length(otherclasses)
          sel=vis.class==otherclasses(i);
          fprintf('Class %d at (%.2f,%.2f) not assigned. ', otherclasses(i), mean(vis.xy(sel,:),1));
          if all(vis.xy(sel,2))>0 && all(vis.range(sel)<obj.maxrange)
            obj.tracks=[obj.tracks,Person(obj.nextid,vis,otherclasses(i))];
            obj.nextid=obj.nextid+1;
          else
            fprintf('Ignoring since it is partially out of range\n');
          end
        end
      end
  
      obj.deleteLostPeople();
    end
  
    function deleteLostPeople(obj)
      if isempty(obj.tracks)
        return;
      end
      
      invisibleForTooLong = 50;
      ageThreshold = 20;
      
      % compute the fraction of the track's age for which it was visible
      ages = [obj.tracks(:).age];
      totalVisibleCounts = [obj.tracks(:).totalVisibleCount];
      visibility = totalVisibleCounts ./ ages;
      
      % find the indices of 'lost' people
      lostInds = (ages < ageThreshold & visibility < 0.6) | ...
          [obj.tracks(:).consecutiveInvisibleCount] >= invisibleForTooLong;
      outsideInds = arrayfun(@(z) ~isempty(z.position) && (norm(z.position) > obj.maxrange || z.position(2)<0),obj.tracks);
      if sum(outsideInds)>0
        fprintf('Deleting %d people out of range\n', sum(outsideInds));
      end
      fl=find(lostInds|outsideInds);
      for i=1:length(fl)
        t=obj.tracks(fl(i));
        fprintf('Deleting track %d at (%.2f,%.2f) with age %d, total %d, consec invis %d\n', ...
                t.id,t.position,t.age, t.totalVisibleCount,t.consecutiveInvisibleCount);
      end

      % delete lost people
      obj.tracks = obj.tracks(~(lostInds|outsideInds));
    end

  end % methods

end % class