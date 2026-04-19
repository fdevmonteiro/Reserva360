import { useState, useEffect } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from '@/components/ui/dialog';
import { getAvailableSalas } from '@/services/salaService';
import { Sala } from '@/types/sala';
import { Reserva } from '@/types/reserva';
import { ReservaFormData } from '@/services/reservaService';
import { Loader2 } from 'lucide-react';
import { Checkbox } from './ui/checkbox';

interface NovaReservaFormProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (data: ReservaFormData, id?: number) => void;
  isLoading: boolean;
  reservaToEdit: Reserva | null;
}

const initialState = {
  title: '',
  description: '',
  startTime: '',
  endTime: '',
  organizerEmail: '',
  salaId: '',
  needsSlideClicker: false,
  createZoomMeeting: false,
};

const NovaReservaForm = ({ isOpen, onClose, onSubmit, isLoading, reservaToEdit }: NovaReservaFormProps) => {
  const isEditMode = Boolean(reservaToEdit);

  const [formData, setFormData] = useState(initialState);
  const [availableRooms, setAvailableRooms] = useState<Sala[]>([]);
  const [isFetchingRooms, setIsFetchingRooms] = useState(false);

  const handleCheckboxChange = (name: 'needsSlideClicker' | 'createZoomMeeting', checked: boolean) => {
    setFormData(prev => ({ ...prev, [name]: checked }));
  };
  
  useEffect(() => {
    if (!isOpen) {
      setFormData(initialState);
      return;
    }

    if (isEditMode && reservaToEdit) {
      setFormData({
        title: reservaToEdit.title,
        description: reservaToEdit.description || '',
        startTime: reservaToEdit.startTime.substring(0, 16),
        endTime: reservaToEdit.endTime.substring(0, 16),
        organizerEmail: reservaToEdit.organizerEmail,
        salaId: String(reservaToEdit.sala.id),
        needsSlideClicker: reservaToEdit.needsSlideClicker ?? false,
        createZoomMeeting: reservaToEdit.createZoomMeeting ?? false,
      });
    } else {
      setFormData(initialState);
    }
  }, [reservaToEdit, isOpen, isEditMode]);

  useEffect(() => {
    const fetchRooms = async () => {
      if (formData.startTime && formData.endTime && new Date(formData.endTime) > new Date(formData.startTime)) {
        setIsFetchingRooms(true);
        try {
          const rooms = await getAvailableSalas(formData.startTime, formData.endTime);
          setAvailableRooms(rooms);
          if (isEditMode && reservaToEdit && !rooms.some(room => room.id === reservaToEdit.sala.id)) {
            setAvailableRooms(prevRooms => [reservaToEdit.sala, ...prevRooms]);
          }
        } catch (error) {
          console.error("Erro ao buscar salas disponíveis:", error);
          setAvailableRooms([]);
        } finally {
          setIsFetchingRooms(false);
        }
      } else {
        setAvailableRooms([]);
      }
    };
    const timer = setTimeout(fetchRooms, 300);
    return () => clearTimeout(timer);
  }, [formData.startTime, formData.endTime, reservaToEdit, isEditMode]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    setFormData(prev => ({ ...prev, [e.target.name]: e.target.value }));
  };

  const handleSalaChange = (salaId: string) => {
    setFormData(prev => ({ ...prev, salaId }));
  };

  // CORREÇÃO: A função agora envia todos os dados do formulário
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.salaId) {
      alert("Por favor, selecione uma sala disponível.");
      return;
    }
    const { salaId, ...restOfData } = formData;
    const selectedSala = availableRooms.find(sala => sala.id === Number(salaId)) || reservaToEdit?.sala;

    const submissionData: ReservaFormData = {
      ...restOfData,
      sala: {
        id: Number(salaId),
        nome: selectedSala?.nome || ''
      },
      createdByUsername: reservaToEdit?.createdByUsername || '', 
    };
    
    onSubmit(submissionData, reservaToEdit?.id);
  };

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{isEditMode ? 'Editar Reserva' : 'Nova Reserva de Sala'}</DialogTitle>
          <DialogDescription>
            {isEditMode ? "Modifique os detalhes da sua reserva." : "Preencha o período para ver as salas disponíveis."}
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4 pt-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label htmlFor="startTime">Início *</Label>
              <Input id="startTime" name="startTime" type="datetime-local" value={formData.startTime} onChange={handleChange} required disabled={isLoading} />
            </div>
            <div>
              <Label htmlFor="endTime">Fim *</Label>
              <Input id="endTime" name="endTime" type="datetime-local" value={formData.endTime} onChange={handleChange} required disabled={isLoading} />
            </div>
          </div>
          <div>
            <Label htmlFor="salaId">Sala Disponível *</Label>
            <Select onValueChange={handleSalaChange} value={formData.salaId} required disabled={isFetchingRooms || isLoading}>
              <SelectTrigger>
                <SelectValue placeholder={isFetchingRooms ? "Buscando..." : "Selecione uma sala"} />
              </SelectTrigger>
              <SelectContent>
                {availableRooms.length > 0 ? (
                  availableRooms.map(sala => (
                    <SelectItem key={sala.id} value={String(sala.id)}>{sala.nome}</SelectItem>
                  ))
                ) : (
                  <div className="p-2 text-sm text-gray-500">Nenhuma sala disponível.</div>
                )}
              </SelectContent>
            </Select>
          </div>
          <div>
            <Label htmlFor="title">Assunto *</Label>
            <Input id="title" name="title" value={formData.title} onChange={handleChange} required disabled={isLoading} />
          </div>
          <div>
            <Label htmlFor="organizerEmail">Email do Organizador *</Label>
            <Input id="organizerEmail" name="organizerEmail" type="email" value={formData.organizerEmail} onChange={handleChange} required disabled={isLoading} />
          </div>
          <div>
            <Label htmlFor="description">Descrição</Label>
            <Textarea id="description" name="description" value={formData.description} onChange={handleChange} disabled={isLoading} />
          </div>
          
          {/* CORREÇÃO: Estrutura dos checkboxes e conexão com o estado */}
          <div className="space-y-4 pt-4 border-t">
            <div className="flex items-center space-x-2">
              <Checkbox 
                id="needsSlideClicker"
                checked={formData.needsSlideClicker}
                onCheckedChange={(checked) => handleCheckboxChange('needsSlideClicker', Boolean(checked))}
              />
              <Label htmlFor="needsSlideClicker" className="font-normal cursor-pointer">
                Precisa de passador de slide?
              </Label>
            </div>
            <div className="flex items-center space-x-2">
              <Checkbox 
                id="createZoomMeeting"
                checked={formData.createZoomMeeting}
                onCheckedChange={(checked) => handleCheckboxChange('createZoomMeeting', Boolean(checked))}
              />
              <Label htmlFor="createZoomMeeting" className="font-normal cursor-pointer">
                Criar também uma sala online no Zoom para esta reserva?
              </Label>
            </div>
          </div>
          
          <DialogFooter className="pt-4">
            <Button type="button" variant="outline" onClick={onClose} disabled={isLoading}>Cancelar</Button>
            <Button type="submit" disabled={isLoading || !formData.salaId}>
              {isLoading ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : (isEditMode ? 'Salvar Alterações' : 'Criar Reserva')}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
};

export default NovaReservaForm;